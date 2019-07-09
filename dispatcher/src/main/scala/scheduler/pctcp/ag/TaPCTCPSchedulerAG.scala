package scheduler.pctcp.ag

import akka.dispatch._
import akka.dispatch.util.CmdLineUtils
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.MessageId
import pctcp.{ChainId, TaPCTCPOptions}
import scheduler.pctcp.PCTCPScheduler
import scheduler.pctcp.ag.AGChainPartitioner.Node

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class TaPCTCPSchedulerAG(options: TaPCTCPOptions) extends PCTCPScheduler with LazyLogging {
  private val randInt = new Random(options.randomSeed)
  private val priorityChangePts: List[Int] =  getRandomChangePoints(options.bugDepth-1)
  private var numCurrentChangePt: Int = 0 // no priority change yet
  CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Priority inversion points at messages: " + priorityChangePts)

  private val partitioner = new AGChainPartitioner()

  // chains are sorted so that the highest in the index has the highest priority:
  private var highPriorityChains: ListBuffer[ChainId] = ListBuffer[ChainId]()
  // lower priorities keep the chains with priorities 0 to d-2
  private var reducedPriorityChains: Array[ChainId] = Array.fill(options.bugDepth-1)(-1)
  private val priorityChangedAt: ListBuffer[MessageId] = new ListBuffer()

  private var last: Map[ChainId, Int] = Map().withDefaultValue(0)
  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)
  private var maxNumAvailableChains = 0 // for stats

  private var allEvents: mutable.Map[MessageId, InternalProgramEvent] = mutable.Map()

  private var currentNumRacyEvents = 0
  private var racyEvents: ListBuffer[MessageId] = ListBuffer(0)

  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    events.filter(_._2.isInstanceOf[MessageSent]).foreach(e => allEvents.put(e._1, e._2)) //for now, only messages sent are considered
    predecessors.toList.sortBy(_._1).foreach(m => partitioner.insert(Node(m._1, m._2)))

    val chains = partitioner.getChains
    val newChains = chains.map(_.id).toSet.diff(highPriorityChains.toSet).diff(reducedPriorityChains.toSet)
    newChains.foreach(c =>
    { // assign random priority
      val pos = randInt.nextInt(highPriorityChains.size + 1)
      // insert after "scheduler.pos" number of chains
      highPriorityChains.insert(pos, c)
    })

    val availableChains = getNumAvailableChains
    if(maxNumAvailableChains < availableChains) maxNumAvailableChains = availableChains
  }

  def scheduleNextMessage: Option[MessageId] = getCurrentChain match {
    case Some(chainId) => Some(schedule(chainId))
    case None => None
  }

  /**
    * Finds the chain to be scheduled next
    * @return chain id of the current chain
    */
  def getCurrentChain: Option[ChainId] = highPriorityChains.toList.reverse.find(c => next(c).isDefined && next(c).get.preds.forall(x => schedule.toSet.contains(x))) match {
    case Some(chainId) => Some(chainId)
    case None => reducedPriorityChains.toList.reverse.find(c => next(c).isDefined) match {
      case Some(chainId) => Some(chainId)
      case None => None
    }
  }

  /**
    * @return true if racy to a concurrently enabled event
    */
  private def isRacy(message: MessageId): Boolean =
    partitioner.getChains.map(c => next(c.id)).filter(e => e.isDefined)
      .exists(m => m.get.id != message && InternalProgramEvent.areRacyEvents(allEvents(m.get.id), allEvents(message)))

  /**
    * @return true if marked as a racy event in the configuration
    */
  private def isMarkedRacy(message: MessageId): Boolean = {
    val content = InternalProgramEvent.getContent(allEvents(message))
    options.racyMessages.exists(str => content.contains(str))
  }

  private def schedule(chainId: ChainId): MessageId = {
    require(next(chainId).isDefined)
    val nextMsg = next(chainId).get.id

    //if(!priorityChangedAt.contains(nextMsg) && !racyEvents.contains(nextMsg) && (isRacy(nextMsg) || isMarkedRacy(nextMsg))) {
    if(!racyEvents.contains(nextMsg) && isMarkedRacy(nextMsg)) {
      racyEvents.append(nextMsg)
      currentNumRacyEvents += 1
    }

    if(priorityChangePts.contains(currentNumRacyEvents) && !priorityChangedAt.contains(nextMsg) ) {
      CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "Changing priority of chain: " + chainId + " to " + priorityChangePts.indexOf(currentNumRacyEvents))
      //println("Next: " + nextMsg + " event: " + allEvents(nextMsg))
      //println("Changing priority of chain: " + chainId + " to " + priorityChangePts.indexOf(currentNumRacyEvents))
      //println("Chains: ")
      //getChains.foreach(c => println(c))
      //println("Messages: ")
      //allEvents.keys.filter(allEvents(_).isInstanceOf[MessageSent]).foreach(k => println(k + " ->" + allEvents(k).asInstanceOf[MessageSent].msg.message))

      highPriorityChains = highPriorityChains.-(chainId)

      // if its priority was reduced before, clean its prev index
      val prevIndex = reducedPriorityChains.indexWhere(_ == chainId)
      if(prevIndex != -1) reducedPriorityChains(prevIndex) = -1

      reducedPriorityChains(priorityChangePts.indexOf(currentNumRacyEvents)) = chainId
      priorityChangedAt.append(nextMsg)
      // CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "Chains ordered by priority after: " + highPriorityChains.toList.reverse + reducedPriorityChains.toList.reverse)

      // ensured to have at least one enabled chain (we have at least one message)
      assert(getCurrentChain.isDefined)
      return schedule(getCurrentChain.get)
    }

    schedule += nextMsg
    numScheduled = numScheduled + 1
    last += (chainId -> (last(chainId) + 1))

    nextMsg
  }

  // index of the next element in the chain
  def next(chainId: ChainId): Option[Node] = {
    val chain = partitioner.getChain(chainId)
    chain match {
      case Some(c) if c.elems.size > last(chainId) => Some(c.elems(last(chainId)))
      case _ => None
    }
  }

  private def getRandomChangePoints(numChPoints: Int): List[Int] = {
    var counter = 1
    var allPossibleChPoints = new ListBuffer[Int]()

    (1 until options.maxRacyMessages).foreach( i => {
      allPossibleChPoints.append(counter)
      counter += 1
    })

    def getRandomFromList: Int = {
      val v = randInt.nextInt(allPossibleChPoints.length)
      allPossibleChPoints.remove(v)
      v
    }

    (0 until numChPoints).toList.map(i => getRandomFromList)
  }

  def getPriorities: List[ChainId] = (highPriorityChains.toList.reverse ++ reducedPriorityChains.toList.reverse).filter(_ != -1)

  // for testing purposes:
  def getPriorityChangePts: List[Int] = priorityChangePts

  def getChains: List[AGChainPartitioner.Chain] = {
    partitioner.printPartitioning()
    partitioner.getChains
  }

  def getAvailableChains: List[AGChainPartitioner.Chain] =
    partitioner.getChains.filter(c => next(c.id).isDefined && next(c.id).get.preds.forall(x => schedule.toSet.contains(x)))

  def getNumAvailableChains: Int = getAvailableChains.size

  def getSchedule: List[MessageId] = schedule.toList
  // the messages at which the priority will be inverted
  def getPrioInvPoints: List[Int] = priorityChangePts.sorted
  def getNumScheduledMsgs: Int = numScheduled
  def getNumChains: Int = partitioner.getChains.size
  def getChainsOfMsgs: List[List[MessageId]] = partitioner.getChains.map(c => c.elems.map(node => node.id))

  def getMaxNumAvailableChains: Int = maxNumAvailableChains
}

