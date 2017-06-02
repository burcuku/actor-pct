package pct

import org.scalatest.{Matchers, WordSpec}
import scala.collection.mutable

class PCTDecompositionTest extends WordSpec with Matchers {

  val pctOptions = PCTOptions(0L)
  
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
  
  "extend" should {
    "place a message greedily into a chain" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      
      pctDecomposition.extend(List(message3.id))
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message3.id)
      
      pctDecomposition.extend(List(message4.id))
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message4.id)

      pctDecomposition.extend(List(message0.id))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message4.id)
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message0.id)      
      
      pctDecomposition.extend(List(message1.id))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message4.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message1.id)
    
      pctDecomposition.extend(List(message2.id))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message4.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)

      pctDecomposition.extend(List(message5.id))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message5.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)

      pctDecomposition.extend(List(message7.id))
      pctDecomposition.getChains.length shouldBe 3
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message5.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)
      pctDecomposition.getChains(2).head shouldBe Some(message7.id)
      pctDecomposition.getChains(2).tail shouldBe Some(message7.id)
    
    }
  }
  
  "minimizeChains" should {
    "test1: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message7.id, message8.id, message6.id, message9.id))  
      pctDecomposition.getChains.length shouldBe 3
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)
      pctDecomposition.getChains(2).head shouldBe Some(message7.id)
      pctDecomposition.getChains(2).tail shouldBe Some(message9.id)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message9.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message6.id)      
    }
    
    "test2: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message6.id))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)      
    }

    "test3: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message7.id, message4.id, message8.id, message9.id))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message9.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message4.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message4.id)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message9.id)      
    }    
  }
  
  "shuffleChains" should {
    "permute chains randomly" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message6.id))  
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message3.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message0.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message2.id)      
      
      pctDecomposition.shuffleChains
      pctDecomposition.getChains.length shouldBe 2
      //pctDecomposition.getChains(0).head should not equal Some(message3.id)      
    }
  }
  
  "getMinEnabledMessage" should {
    "compute the first enabled message in the list of chains" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message7.id, message8.id, message6.id, message9.id))        
      
      pctDecomposition.getMinEnabledMessage shouldBe pctDecomposition.getChains(0).head
      pctDecomposition.getMinEnabledMessage
      pctDecomposition.getMinEnabledMessage
      pctDecomposition.getMinEnabledMessage
      pctDecomposition.getMinEnabledMessage
      pctDecomposition.getMinEnabledMessage shouldBe pctDecomposition.getChains(1).head
    }
  }
  
  "decreasePriority" should {
    "move the chain of a message to the end of chains list" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message7.id, message8.id, message6.id, message9.id))        
      val id = pctDecomposition.getMinEnabledMessage.get
      pctDecomposition.decreasePriority(id)
      pctDecomposition.getChains.last.contains(id) shouldBe true
    }
  }
}
