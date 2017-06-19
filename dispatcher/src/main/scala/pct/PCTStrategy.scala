package pct


import com.typesafe.scalalogging.LazyLogging
import scala.util.Random
import scala.collection.mutable


class PCTStrategy(options: PCTOptions) extends LazyLogging {
  private var msgIndex: Int = 0
  private val randInt = new Random //new Random(options.randomSeed)

  private val pctDecomposition = new PCTDecomposition(options)
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
  
  def setNewMessages(predecessors: Map[MessageId, Set[MessageId]]) = {
    logPredecessors(predecessors)
    pctDecomposition.putMessages(predecessors)
    pctDecomposition.extend(predecessors.keys.toList)
    pctDecomposition.minimizeChains
    pctDecomposition.shuffleChains
  }
  
  def getNextMessage: Option[MessageId] = {
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
}
