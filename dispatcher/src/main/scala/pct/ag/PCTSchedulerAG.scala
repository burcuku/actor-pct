package pct.ag

import pct.ag.ChainPartitioner.Node
import pct.{ChainId, MessageId, PCTOptions, PCTScheduler}

import scala.collection.mutable.ListBuffer
import scala.util.Random

class PCTSchedulerAG(pctOptions: PCTOptions) extends PCTScheduler {
  private val randInt = new Random(pctOptions.randomSeed)
  private val HIGH = 1
  private val LOW = 0

  private val priorityChangePts: Set[Int] = Array.fill(pctOptions.bugDepth-1)(randInt.nextInt(pctOptions.maxMessages)).toSet
  private val partitioner = new ChainPartitioner()

  private var priorities: Array[ListBuffer[ChainId]] = Array.fill[ListBuffer[ChainId]](2)(ListBuffer[ChainId]())
  private var last: Map[ChainId, Int] = Map().withDefaultValue(0)
  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)

  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    predecessors.foreach(m => partitioner.insert(Node(m._1, m._2)))

    val chains = partitioner.getChains
    val newChains = chains.map(_.id).toSet.diff(priorities(LOW).toSet).diff(priorities(HIGH).toSet)
    newChains.foreach(c => assignPriority(c, high = true))
  }

  def getNextMessage: Option[MessageId] = {
    priorities(HIGH).find(c => next(c).isDefined && next(c).get.preds.forall(x => schedule.toSet.contains(x))) match {
      // schedule an updated-priority chain
      case Some(chainId) => Some(schedule(chainId))
      case None => priorities(LOW).find(c => next(c).isDefined) match {
        // schedule a chain with its initial priority
        case Some(chainId) => Some(schedule(chainId))
        // no more chains with next elements
        case None => None
      }
    }
  }

  private def schedule(chainId: ChainId): MessageId = {
    require(next(chainId).isDefined)
    val nextMsg = next(chainId).get.id
    schedule += nextMsg
    last += (chainId -> (last(chainId) + 1))
    if(priorityChangePts.contains(numScheduled)) {
      assignPriority(chainId, high = false)
    }
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

  def assignPriority(id: ChainId, high: Boolean): Unit = {
    if(high) {
      // assign random priority
      val pos = randInt.nextInt(priorities(HIGH).size + 1)
      // insert after "pos" number of chains
      priorities(HIGH).insert(pos, id)
    } else {
      // decrease priority - add to the lower priority buffer
      priorities(HIGH) = priorities(HIGH).-(id)
      priorities(LOW).insert(priorities(LOW).size, id)
    }
  }

  def getPriorities(high: Boolean): List[ChainId] = if(high) priorities(HIGH).toList else priorities(LOW).toList

  // for testing purposes:
  def getHighestPriorityChain(high: Boolean): ChainId = if(high) priorities(HIGH).toList.head else priorities(LOW).toList.head
  def getPriorityChangePts: Set[Int] = priorityChangePts

  def getChains: List[ChainPartitioner.Chain] = {
    partitioner.printPartitioning()
    partitioner.getChains
  }

  def getSchedule: List[MessageId] = schedule.toList
}