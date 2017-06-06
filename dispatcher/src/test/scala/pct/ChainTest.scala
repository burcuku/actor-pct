package pct

import org.scalatest.{FlatSpec, Matchers}

class ChainTest extends FlatSpec with Matchers {

  val message1 = Message(1L, Set())
  val message2 = Message(2L, Set())
  val message3 = Message(3L, Set(2L))

  List(message1, message2, message3).foreach(m => Messages.putMessage(m))

  it should "add messages to an empty chain and update head/tail correctly" in {
    val chain = new Chain()

    chain.head shouldBe None
    chain.tail shouldBe None

    chain.append(message1.id)

    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message1.id)
  }

  it should "add messages to an initialized chain  and update head/tail correctly" in {
    val chain = new Chain()

    chain.append(message2.id)
    chain.append(message3.id)

    chain.head shouldBe Some(message2.id)
    chain.tail shouldBe Some(message3.id)
  }
  
  it should "add a list of messages to an empty chain and update head/tail correctly" in {
    val chain = new Chain()

    chain.head shouldBe None
    chain.tail shouldBe None

    chain.appendAll(List(message1.id, message2.id, message3.id))

    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message3.id)
  }  

  it should "add a list of messages to an initialized chain  and update head/tail correctly" in {
    val chain = new Chain(message1.id)

    chain.appendAll(List(message2.id, message3.id))

    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message3.id)
  }
  
  it should "remove a message from a chain and update head/tail correctly" in {
    val chain = new Chain(message1.id)
    chain.appendAll(List(message2.id, message3.id))
    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message3.id)
    
    chain.remove(message1.id)
    chain.head shouldBe Some(message2.id)
    chain.tail shouldBe Some(message3.id)
    
    chain.remove(message3.id)
    chain.head shouldBe Some(message2.id)
    chain.tail shouldBe Some(message2.id)
    
    chain.remove(message2.id)
    chain.head shouldBe None
    chain.tail shouldBe None    
  }

  it should "remove a list of messages from a chain and update head/tail correctly" in {
    val chain = new Chain(message1.id)
    chain.appendAll(List(message2.id, message3.id))
    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message3.id)
    
    chain.removeAll(List(message2.id, message3.id))
    chain.head shouldBe Some(message1.id)
    chain.tail shouldBe Some(message1.id)    
  }
  
  it should "compute a slice of the chain containing all successors of a message including the message itself" in {
    val chain = new Chain(message1.id)
    chain.appendAll(List(message2.id, message3.id))
    chain.sliceSuccessors(message1.id) shouldBe List(message1.id, message2.id, message3.id)
    chain.sliceSuccessors(message2.id) shouldBe List(message2.id, message3.id)
    chain.sliceSuccessors(message3.id) shouldBe List(message3.id)
  }

  it should "compute the successor of a message" in {
    val chain = new Chain(message1.id)
    chain.appendAll(List(message2.id, message3.id))
    chain.nextMessage(message1.id) shouldBe Some(message2.id)
    chain.nextMessage(message2.id) shouldBe Some(message3.id)
    chain.nextMessage(message3.id) shouldBe None
  }
  
  it should "calculate first enabled message correctly as head when head is enabled" in {
    val chain = new Chain()
    chain.firstEnabled shouldBe None

    chain.append(message2.id)
    chain.firstEnabled shouldBe chain.head
    chain.firstEnabled shouldBe Some(message2.id)

    chain.append(message3.id)
    chain.firstEnabled shouldBe chain.head
    chain.firstEnabled shouldBe Some(message2.id)
  }

  it should "calculate first enabled message correctly when a message is received" in {
    val chain = new Chain()

    chain.append(message2.id)
    chain.firstEnabled shouldBe Some(message2.id)

    chain.append(message3.id)
    chain.firstEnabled shouldBe Some(message2.id)

    message2.received = true
    chain.firstEnabled shouldBe Some(message3.id)
  }

  it should "calculate first enabled message as None when all messages are received" in {
    val chain = new Chain()

    chain.append(message2.id)
    chain.append(message3.id)

    message2.received = true
    message3.received = true

    chain.firstEnabled shouldBe None
  }
    
  /*it should "calculate first unreceived message correctly as head when no messages are received" in {
    val chain = new Chain()
    chain.firstUnreceived shouldBe None

    chain.append(message2)
    chain.firstUnreceived shouldBe chain.head
    chain.firstUnreceived shouldBe Some(message2)

    chain.append(message3)
    chain.firstUnreceived shouldBe chain.head
    chain.firstUnreceived shouldBe Some(message2)
  }

  it should "calculate first unreceived message correctly when a message is received" in {
    val chain = new Chain()

    chain.append(message2)
    chain.firstUnreceived shouldBe Some(message2)

    chain.append(message3)
    chain.firstUnreceived shouldBe Some(message2)

    message2.received = true
    chain.firstUnreceived shouldBe Some(message3)
  }

  it should "calculate first unreceived message as None when all messages are received" in {
    val chain = new Chain()

    chain.append(message2)
    chain.append(message3)

    message2.received = true
    message3.received = true

    chain.firstUnreceived shouldBe None
  }*/
}
