package pct.bm

import org.scalatest.{Matchers, WordSpec}
import pct.PCTOptions

class PCTSchedulerBMTest extends WordSpec with Matchers {
  /*val message0 = Message(20L, Set())
  val message1 = Message(21L, Set(20L))
  val message2 = Message(22L, Set(21L))
  val message3 = Message(23L, Set())
  val message4 = Message(24L, Set(23L))
  val message5 = Message(25L, Set(22L, 24L))
  val message6 = Message(26L, Set(25L))
  val message7 = Message(27L, Set(24L))
  val message8 = Message(28L, Set(27L))
  val message9 = Message(29L, Set(28L))*/

  //List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => Messages.putMessage(m))
  
  "getNextMessage" should {
    "test1: schedule next message according to a randomly assigned priority to its corresponding chain" in {
      val pctOptions = PCTOptions(maxMessages = 10, bugDepth = 1)
      val pctStrategy = new PCTSchedulerBM(pctOptions)
      
      //pctStrategy.setNewMessages(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))
      pctStrategy.addNewMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      pctStrategy.printPrioInvPoints
      
      for (i <- 0 until 10)
        pctStrategy.scheduleNextMessage
        
      pctStrategy.printSchedule   
    }
    
    "test2: schedule next message according to a randomly assigned priority to its corresponding chain" in {
      val pctOptions = PCTOptions(maxMessages = 10, bugDepth = 2)
      val pctStrategy = new PCTSchedulerBM(pctOptions)
      //List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => m.received = false)
              
      //pctStrategy.setNewMessages(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))
      //pctStrategy.setNewMessages(Map(20L->Set(), 21L->Set(20L), 22L->Set(21L), 23L->Set(), 24L->Set(23L), 25L->Set(22L, 24L), 26L->Set(25L), 27L->Set(24L), 28L->Set(27L), 29L->Set(28L)))
      pctStrategy.addNewMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      pctStrategy.printPrioInvPoints
      
      for (i <- 0 until 10)
        pctStrategy.scheduleNextMessage
        
      pctStrategy.printSchedule  
    }
    
    "test3: schedule next message according to a randomly assigned priority to its corresponding chain" in {
      val pctOptions = PCTOptions(maxMessages = 4, bugDepth = 1)
      val pctStrategy = new PCTSchedulerBM(pctOptions)
      //List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => m.received = false)
              
      //pctStrategy.setNewMessages(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))
      //pctStrategy.setNewMessages(Map(20L->Set(), 21L->Set(20L), 22L->Set(21L), 23L->Set(), 24L->Set(23L), 25L->Set(22L, 24L), 26L->Set(25L), 27L->Set(24L), 28L->Set(27L), 29L->Set(28L)))
      pctStrategy.addNewMessages(Map(0L->Set(), 1L->Set(), 2L->Set(0L, 1L), 3L->Set(0L)))
      pctStrategy.printPrioInvPoints
      
      for (i <- 0 until 4)
        pctStrategy.scheduleNextMessage
        
      pctStrategy.printSchedule  
    }        
  }
}