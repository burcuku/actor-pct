package pct

import org.scalatest.{Matchers, WordSpec}
import scala.collection.mutable

class PCTDecompositionTest extends WordSpec with Matchers {

  //val pctOptions = PCTOptions(0L)
  val pctOptions = PCTOptions()
  
  val message0 = Message(30L, Set())
  val message1 = Message(31L, Set(30L))
  val message2 = Message(32L, Set(31L))
  val message3 = Message(33L, Set())
  val message4 = Message(34L, Set(33L))
  val message5 = Message(35L, Set(32L, 34L))
  val message6 = Message(36L, Set(35L))
  val message7 = Message(37L, Set(34L))
  val message8 = Message(38L, Set(37L))
  val message9 = Message(39L, Set(38L))

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
    
    "test4: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message0.id, message1.id, message2.id, message3.id, message4.id, message5.id, message6.id, message7.id, message8.id, message9.id))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message0.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message3.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message9.id)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(message0.id)
      pctDecomposition.getChains(0).tail shouldBe Some(message6.id)      
      pctDecomposition.getChains(1).head shouldBe Some(message3.id)
      pctDecomposition.getChains(1).tail shouldBe Some(message9.id)
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
    }
  }
  
  "getMinEnabledMessage" should {
    "compute the first enabled message in the list of chains" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message7.id, message8.id, message6.id, message9.id))   
      pctDecomposition.minimizeChains
      pctDecomposition.shuffleChains
      
      if (pctDecomposition.getChains(0).head == Some(message0.id)) {
        pctDecomposition.getMinEnabledMessage shouldBe Some(message0.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message1.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message2.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message3.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message4.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message5.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message6.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message7.id)        
      }
      else {
        pctDecomposition.getMinEnabledMessage shouldBe Some(message3.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message4.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message7.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message8.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message9.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message0.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message1.id)
        pctDecomposition.getMinEnabledMessage shouldBe Some(message2.id)                
      }
    }
  }
  
  "decreasePriority" should {
    "move the chain of a message to the end of chains list" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(message3.id, message4.id, message0.id, message1.id, message2.id, message5.id, message7.id, message8.id, message6.id, message9.id))        
      pctDecomposition.minimizeChains
      pctDecomposition.shuffleChains
      val id = pctDecomposition.getMinEnabledMessage.get
      pctDecomposition.decreasePriority(id)
      pctDecomposition.getChains.last.contains(id) shouldBe true
    }
  }
}
