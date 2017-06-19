package pct

import org.scalatest.{Matchers, WordSpec}
import scala.collection.mutable

class PCTDecompositionTest extends WordSpec with Matchers {

  //val pctOptions = PCTOptions(0L)
  val pctOptions = PCTOptions()
  
  /*val message0 = Message(30L, Set())
  val message1 = Message(31L, Set(30L))
  val message2 = Message(32L, Set(31L))
  val message3 = Message(33L, Set())
  val message4 = Message(34L, Set(33L))
  val message5 = Message(35L, Set(32L, 34L))
  val message6 = Message(36L, Set(35L))
  val message7 = Message(37L, Set(34L))
  val message8 = Message(38L, Set(37L))
  val message9 = Message(39L, Set(38L))*/

  //List(message0, message1, message2, message3, message4, message5, message6, message7, message8, message9).foreach(m => Messages.putMessage(m))
  
  "extend" should {
    "place a message greedily into a chain" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      
      pctDecomposition.extend(List(3L))
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(3L)
      
      pctDecomposition.extend(List(4L))
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(4L)

      pctDecomposition.extend(List(0L))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(4L)
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(0L)      
      
      pctDecomposition.extend(List(1L))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(4L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(1L)
    
      pctDecomposition.extend(List(2L))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(4L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)

      pctDecomposition.extend(List(5L))
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(5L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)

      pctDecomposition.extend(List(7L))
      pctDecomposition.getChains.length shouldBe 3
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(5L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)
      pctDecomposition.getChains(2).head shouldBe Some(7L)
      pctDecomposition.getChains(2).tail shouldBe Some(7L)
    
    }
  }
  
  "minimizeChains" should {
    "test1: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 4L, 0L, 1L, 2L, 5L, 7L, 8L, 6L, 9L))  
      pctDecomposition.getChains.length shouldBe 3
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)
      pctDecomposition.getChains(2).head shouldBe Some(7L)
      pctDecomposition.getChains(2).tail shouldBe Some(9L)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(9L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(6L)      
    }
    
    "test2: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 4L, 0L, 1L, 2L, 5L, 6L))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)      
    }

    "test3: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 7L, 4L, 8L, 9L))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(9L)      
      pctDecomposition.getChains(1).head shouldBe Some(4L)
      pctDecomposition.getChains(1).tail shouldBe Some(4L)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 1
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(9L)      
    }  
    
    "test4: minimize the number of chains (width of partial order)" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))  
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(0L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(3L)
      pctDecomposition.getChains(1).tail shouldBe Some(9L)
      
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(0L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(3L)
      pctDecomposition.getChains(1).tail shouldBe Some(9L)
    }    
  }
  
  "shuffleChains" should {
    "permute chains randomly" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 4L, 0L, 1L, 2L, 5L, 6L))  
      pctDecomposition.minimizeChains
      pctDecomposition.getChains.length shouldBe 2
      pctDecomposition.getChains(0).head shouldBe Some(3L)
      pctDecomposition.getChains(0).tail shouldBe Some(6L)      
      pctDecomposition.getChains(1).head shouldBe Some(0L)
      pctDecomposition.getChains(1).tail shouldBe Some(2L)
                  
      pctDecomposition.shuffleChains
      pctDecomposition.getChains.length shouldBe 2
    }
  }
  
  "getMinEnabledMessage" should {
    "compute the first enabled message in the list of chains" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 4L, 0L, 1L, 2L, 5L, 7L, 8L, 6L, 9L))   
      pctDecomposition.minimizeChains
      pctDecomposition.shuffleChains
      
      if (pctDecomposition.getChains(0).head == Some(0L)) {
        pctDecomposition.getMinEnabledMessage shouldBe Some(0L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(1L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(2L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(3L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(4L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(5L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(6L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(7L)        
      }
      else {
        pctDecomposition.getMinEnabledMessage shouldBe Some(3L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(4L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(7L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(8L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(9L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(0L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(1L)
        pctDecomposition.getMinEnabledMessage shouldBe Some(2L)                
      }
    }
  }
  
  "decreasePriority" should {
    "move the chain of a message to the end of chains list" in {
      val pctDecomposition = new PCTDecomposition(pctOptions)
      pctDecomposition.putMessages(Map(0L->Set(), 1L->Set(0L), 2L->Set(1L), 3L->Set(), 4L->Set(3L), 5L->Set(2L, 4L), 6L->Set(5L), 7L->Set(4L), 8L->Set(7L), 9L->Set(8L)))
      //pctDecomposition.getChains shouldBe empty
      pctDecomposition.extend(List(3L, 4L, 0L, 1L, 2L, 5L, 7L, 8L, 6L, 9L))        
      pctDecomposition.minimizeChains
      pctDecomposition.shuffleChains
      val id = pctDecomposition.getMinEnabledMessage.get
      pctDecomposition.decreasePriority(id)
      pctDecomposition.getChains.last.contains(id) shouldBe true
    }
  }
}
