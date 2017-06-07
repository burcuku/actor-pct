package pct

import scala.util.Random
import scala.collection.mutable


class PCTStrategy(options: PCTOptions) {
  private var msgIndex: Int = 0
  private val randInt = new Random //new Random(options.randomSeed)

  private val decomposition = new PCTDecomposition(options)
  /*private val prioInvPoints: List[Int] = (0 until options.bugDepth)
    .map(i => randInt.nextInt(options.maxMessages))
    .toSet*/  
  private var prioInvPoints: Set[Int] = Set()
  private def setPrioInvPoints: Unit = if (prioInvPoints.size < options.bugDepth - 1) {
    prioInvPoints += randInt.nextInt(options.maxMessages)
    setPrioInvPoints
  }  
  setPrioInvPoints
  
  private val schedule: mutable.ListBuffer[MessageId] = mutable.ListBuffer.empty 
  
  def printPrioInvPoints = {
    println("Priority Inversion Points:")
    prioInvPoints.foreach(p => println(p))
  }
  
  def printSchedule = { 
    println("Schedule:")
    schedule.foreach(id => println(id))
  }
  
  def setNewMessages(ids: List[MessageId]) = {
    decomposition.extend(ids)
    decomposition.minimizeChains
    decomposition.shuffleChains
  }
  
  def getNextMessage: Option[MessageId] = {
    val nextId = decomposition.getMinEnabledMessage()
    nextId match {
      case Some(id) =>
        if (prioInvPoints.contains(msgIndex)) 
          decomposition.decreasePriority(id)
        msgIndex += 1 
        schedule += id
      case _ =>
    }    
    nextId
  }
}
