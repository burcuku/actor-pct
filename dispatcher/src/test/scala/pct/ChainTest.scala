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

    chain.append(message1)

    chain.head shouldBe Some(message1)
    chain.tail shouldBe Some(message1)
  }

  it should "add messages to an initialized chain  and update head/tail correctly" in {
    val chain = new Chain()

    chain.append(message2)
    chain.append(message3)

    chain.head shouldBe Some(message2)
    chain.tail shouldBe Some(message3)
  }

  it should "calculate first unreceived message correctly as head when no messages are received" in {
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
  }
}
