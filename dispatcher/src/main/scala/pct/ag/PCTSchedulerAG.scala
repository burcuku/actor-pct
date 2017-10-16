package pct.ag

import com.typesafe.scalalogging.LazyLogging
import pct.ag.ChainPartitioner.Node
import pct.{ChainId, MessageId, PCTOptions, PCTScheduler}

import scala.collection.mutable.ListBuffer
import scala.util.Random

class PCTSchedulerAG(pctOptions: PCTOptions) extends PCTScheduler with LazyLogging {
  private val randInt = new Random(pctOptions.randomSeed)
  private val priorityChangePts: Set[MessageId] = Array.fill(pctOptions.bugDepth-1)(randInt.nextInt(pctOptions.maxMessages).asInstanceOf[MessageId]).toSet
  private var numCurrentChangePt: Int = 0 // no priority change yet
  logger.info("Priority inversion points at messages: " + priorityChangePts)

  private val partitioner = new ChainPartitioner()

  // chains are sorted so that the highest in the index has the highest priority:
  private var highPriorityChains: ListBuffer[ChainId] = ListBuffer[ChainId]()
  // lower prioroties keep the chains with priorities 0 to d-2
  private var reducedPriorityChains: Array[ChainId] = Array.fill(pctOptions.bugDepth-1)(-1)

  private var last: Map[ChainId, Int] = Map().withDefaultValue(0)
  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)

  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    predecessors.toList.sortBy(_._1).foreach(m => partitioner.insert(Node(m._1, m._2)))

    val chains = partitioner.getChains
    val newChains = chains.map(_.id).toSet.diff(highPriorityChains.toSet).diff(reducedPriorityChains.toSet)
    newChains.foreach(c =>
      { // assign random priority
        val pos = randInt.nextInt(highPriorityChains.size + 1)
        // insert after "pos" number of chains
        highPriorityChains.insert(pos, c)
      })
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
    logger.debug("Priority change at message: " + nextMsg)

    if(priorityChangePts.contains(nextMsg) && priorityChangePts.size != numCurrentChangePt && reducedPriorityChains(priorityChangePts.size - numCurrentChangePt - 1) != chainId) {
      logger.debug("Changing priority of chain: " + chainId + " to " + (priorityChangePts.size - numCurrentChangePt - 1))
      highPriorityChains = highPriorityChains.-(chainId)

      // if its priority was reduces before, clean its prev index
      val prevIndex = reducedPriorityChains.indexWhere(_ == chainId)
      if(prevIndex != -1) reducedPriorityChains(prevIndex) = -1

      reducedPriorityChains(priorityChangePts.size - numCurrentChangePt - 1) = chainId
      numCurrentChangePt += 1
      logger.debug("Chains ordered by priority after: " + highPriorityChains.toList.reverse + reducedPriorityChains.toList.reverse)

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
  def getPriorityChangePts: Set[MessageId] = priorityChangePts

  def getChains: List[ChainPartitioner.Chain] = {
    partitioner.printPartitioning()
    partitioner.getChains
  }

  def getSchedule: List[MessageId] = schedule.toList
  // the messages at which the priority will be inverted
  def getPrioInvPoints: List[Int] = priorityChangePts.toList.sorted.map(i => i.asInstanceOf[Int])
  def getNumScheduledMsgs: Int = numScheduled
  def getNumChains: Int = partitioner.getChains.size
  def getChainsOfMsgs: List[List[MessageId]] = partitioner.getChains.map(c => c.elems.map(node => node.id))
}