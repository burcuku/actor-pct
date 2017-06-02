package pct

import org.scalatest.{FlatSpec, Matchers}

class MessagesTest extends FlatSpec with Matchers {

  val message1 = Message(1L, Set())
  val message2 = Message(2L, Set(), received = true)
  val message3 = Message(3L, Set(2L), received = true)
  val message4 = Message(4L, Set(1L, 2L))
  val message5 = Message(5L, Set(1L, 3L))

  List(message1, message2, message3, message4, message5).foreach(m => Messages.putMessage(m))

  "all preds" should "correctly return predecessors" in {
    Messages.allPreds(1L) shouldBe Set()
    Messages.allPreds(3L) shouldBe Set(2L)
    Messages.allPreds(4L) shouldBe Set(1L, 2L)
    Messages.allPreds(5L) shouldBe Set(1L, 2L, 3L)
  }

  "enabled" should "correctly return whether a message is enabled" in {
    Messages.isEnabled(1L) shouldBe true
    Messages.isEnabled(2L) shouldBe false
    Messages.isEnabled(3L) shouldBe false
    Messages.isEnabled(4L) shouldBe false
    Messages.isEnabled(5L) shouldBe false
  }

  "before" should "correctly return whether a message is causally before another" in {
    Messages.isBefore(1L, 1L) shouldBe false
    Messages.isBefore(1L, 2L) shouldBe false

    Messages.isBefore(2L, 3L) shouldBe true
    Messages.isBefore(3L, 2L) shouldBe false

    Messages.isBefore(4L, 3L) shouldBe false
    Messages.isBefore(4L, 1L) shouldBe false
    Messages.isBefore(4L, 2L) shouldBe false
    Messages.isBefore(2L, 4L) shouldBe true
    Messages.isBefore(1L, 4L) shouldBe true

    Messages.isBefore(5L, 1L) shouldBe false
    Messages.isBefore(1L, 5L) shouldBe true
    Messages.isBefore(2L, 5L) shouldBe true
    Messages.isBefore(3L, 5L) shouldBe true
  }
}
