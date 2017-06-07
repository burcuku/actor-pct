package pct

import org.scalatest.{Matchers, WordSpec}
import scala.collection.mutable

class PCTStrategyTest extends WordSpec with Matchers {  
  val message0 = Message(0L, Set())
  val message1 = Message(1L, Set(0L))
  val message2 = Message(2L, Set(1L))
  val message3 = Message(3L, Set())
  val message4 = Message(4L, Set(3L))
  val message5 = Message(5L, Set(2L, 4L))
  val message6 = Message(6L, Set(5L))
  val message7 = Message(7L, Set(4L))
  val message8 = Message(8L, Set(7L))
  val message9 = Message(9L, Set(8L))

  List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => Messages.putMessage(m))
  
  "getNextMessage" should {
    "test1: schedule next message according to a randomly assigned priority to its corresponding chain" in {
      val pctOptions = PCTOptions(maxMessages = 10, bugDepth = 1)
      val pctStrategy = new PCTStrategy(pctOptions)
      
      pctStrategy.setNewMessages(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))
      pctStrategy.printPrioInvPoints
      
      for (i <- 0 until 10)
        pctStrategy.getNextMessage
        
      pctStrategy.printSchedule   
    }
    
    "test2: schedule next message according to a randomly assigned priority to its corresponding chain" in {
      val pctOptions = PCTOptions(maxMessages = 10, bugDepth = 2)
      val pctStrategy = new PCTStrategy(pctOptions)
      List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => m.received = false)
              
      pctStrategy.setNewMessages(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))
      pctStrategy.printPrioInvPoints
      
      for (i <- 0 until 10)
        pctStrategy.getNextMessage
        
      pctStrategy.printSchedule  
    }    
  }
}