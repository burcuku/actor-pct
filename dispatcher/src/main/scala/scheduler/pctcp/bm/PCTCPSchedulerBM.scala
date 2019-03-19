package scheduler.pctcp.bm

import com.typesafe.scalalogging.LazyLogging
import pctcp.PCTCPOptions
import scheduler.Scheduler.MessageId
import scheduler.pctcp.PCTCPScheduler

import scala.collection.mutable
import scala.util.Random


class PCTCPSchedulerBM(options: PCTCPOptions) extends PCTCPScheduler with LazyLogging {
  private var msgIndex: Int = 0
  private val randInt = new Random //new Random(options.randomSeed)

  private val pctDecomposition = new PCTDecomposition(options)
  addNewMessages(Map(0L->Set()))
  /*private val prioInvPoints: List[Int] = (0 until options.bugDepth)
    .map(i => randInt.nextInt(options.maxMessages))
    .toSet*/  
  private var prioInvPoints: Set[Int] = Set()
  private def setPrioInvPoints: Unit = if (prioInvPoints.size < options.bugDepth - 1) {
    prioInvPoints += randInt.nextInt(options.maxMessages)
    setPrioInvPoints
  }  
  setPrioInvPoints
  logger.info("Priority Inversion Points: " + prioInvPoints + "\n")
  
  private val schedule: mutable.ListBuffer[MessageId] = mutable.ListBuffer.empty
  
  private def logSchedule(id: MessageId) = {
    logger.info("chains:")
    pctDecomposition.getChains.foreach (c => logger.info("\t" + c.toList))
    logger.info("schedule[" + msgIndex + "]= " + id)    
  }
  
  private def logPredecessors(predecessors: Map[MessageId, Set[MessageId]]) = {
    logger.info("PCTStrategy received predecessors: ")
    predecessors.foreach(m => logger.info("\t" + "Message " + m._1 + " has predecessors -> " + m._2))
  }
  
  def printPrioInvPoints = {
    println("Priority Inversion Points:")
    prioInvPoints.foreach(p => println(p))
  }
  
  def printSchedule = { 
    println("Schedule:")
    schedule.foreach(id => println(id))
  }
  
  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]) = {
    logPredecessors(predecessors)
    pctDecomposition.putMessages(predecessors)
    pctDecomposition.extend(predecessors.keys.toList)
    pctDecomposition.minimizeChains
    pctDecomposition.shuffleChains
  }
  
  def scheduleNextMessage: Option[MessageId] = {
    val nextId = pctDecomposition.getMinEnabledMessage()
    nextId match {
      case Some(id) =>
        if (prioInvPoints.contains(msgIndex)) 
          pctDecomposition.decreasePriority(id)
        logSchedule(id)
        msgIndex += 1 
        schedule += id
      case _ =>
    }    
    nextId
  }

  def getSchedule: List[MessageId] = schedule.toList
  def getPrioInvPoints: List[Int] = prioInvPoints.toList.sorted
  def getNumScheduledMsgs: Int = msgIndex
  def getNumChains: Int = pctDecomposition.getChains.size
  def getChainsOfMsgs: List[List[MessageId]] = pctDecomposition.getChains.map(x => x.toList)

  // todo update with the size of chains with enabled events:
  def getMaxNumAvailableChains: Int = 0

}
