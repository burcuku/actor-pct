package scheduler.pctcp.ag

import akka.dispatch.InternalProgramEvent
import akka.dispatch.util.CmdLineUtils
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.MessageId
import pctcp.{ChainId, PCTCPOptions}
import scheduler.pctcp.PCTCPScheduler
import scheduler.pctcp.ag.AGChainPartitioner.Node

import scala.collection.mutable.ListBuffer
import scala.util.Random

class PCTCPSchedulerAG(pctcpOptions: PCTCPOptions) extends PCTCPScheduler with LazyLogging {
  private val randInt = new Random(pctcpOptions.randomSeed)
  private val priorityChangePts: List[MessageId] =  getRandomChangePoints(pctcpOptions.bugDepth-1)
  CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Priority inversion points at messages: " + priorityChangePts)
  private val partitioner = new AGChainPartitioner()

  // chains are sorted so that the highest in the index has the highest priority:
  private var highPriorityChains: ListBuffer[ChainId] = ListBuffer[ChainId]()
  // lower priorities keep the chains with priorities 0 to d-2
  private var reducedPriorityChains: Array[ChainId] = Array.fill(pctcpOptions.bugDepth-1)(-1)
  private val priorityChangedAt: ListBuffer[MessageId] = new ListBuffer()

  private var last: Map[ChainId, Int] = Map().withDefaultValue(0)
  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)
  private var maxNumAvailableChains = 0 // for stats

  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
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

  private def schedule(chainId: ChainId): MessageId = {
    require(next(chainId).isDefined)

    val nextMsg = next(chainId).get.id
    CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Priority change at message: " + nextMsg)

    if(priorityChangePts.contains(nextMsg) && !priorityChangedAt.contains(nextMsg)) {
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Changing priority of chain: " + chainId + " to " + priorityChangePts.indexOf(nextMsg))
      highPriorityChains = highPriorityChains.-(chainId)

      // if its priority was reduces before, clean its prev index
      val prevIndex = reducedPriorityChains.indexWhere(_ == chainId)
      if(prevIndex != -1) reducedPriorityChains(prevIndex) = -1

      reducedPriorityChains(priorityChangePts.indexOf(nextMsg)) = chainId
      priorityChangedAt.append(nextMsg)
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Chains ordered by priority after: " + highPriorityChains.toList.reverse + reducedPriorityChains.toList.reverse)

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

  def getPriorities: List[ChainId] = (highPriorityChains.toList.reverse ++ reducedPriorityChains.toList.reverse).filter(_ != -1)

  // for testing purposes:
  def getPriorityChangePts: List[MessageId] = priorityChangePts

  def getChains: List[AGChainPartitioner.Chain] = {
    partitioner.printPartitioning()
    partitioner.getChains
  }

  private def getRandomChangePoints(numChPoints: Int): List[MessageId] = {
    var counter = 1
    var allPossibleChPoints = new ListBuffer[Int]()

    (0 until pctcpOptions.maxMessages).foreach( i => {
      allPossibleChPoints.append(counter)
      counter += 1
    })

    def getRandomFromList: Int = {
      val v = randInt.nextInt(allPossibleChPoints.length)
      allPossibleChPoints.remove(v)
      v
    }

    (0 until numChPoints).toList.map(i => getRandomFromList.asInstanceOf[MessageId])
  }

  def getAvailableChains: List[AGChainPartitioner.Chain] =
    partitioner.getChains.filter(c => next(c.id).isDefined && next(c.id).get.preds.forall(x => schedule.toSet.contains(x)))

  def getNumAvailableChains: Int = getAvailableChains.size

  def getSchedule: List[MessageId] = schedule.toList
  // the messages at which the priority will be inverted
  def getPrioInvPoints: List[Int] = priorityChangePts.toList.sorted.map(i => i.asInstanceOf[Int])
  def getNumScheduledMsgs: Int = numScheduled
  def getNumChains: Int = partitioner.getChains.size
  def getChainsOfMsgs: List[List[MessageId]] = partitioner.getChains.map(c => c.elems.map(node => node.id))

  def getMaxNumAvailableChains: Int = maxNumAvailableChains
}
