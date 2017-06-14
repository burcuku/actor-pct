package pct

import org.scalatest.{FlatSpec, Matchers}

class MessagesTest extends FlatSpec with Matchers {

  val message1 = Message(11L, Set())
  val message2 = Message(12L, Set(), received = true)
  val message3 = Message(13L, Set(12L), received = true)
  val message4 = Message(14L, Set(11L, 12L))
  val message5 = Message(15L, Set(11L, 13L))

  List(message1, message2, message3, message4, message5).foreach(m => Messages.putMessage(m))

  "all preds" should "correctly return predecessors" in {
    Messages.allPreds(11L) shouldBe Set()
    Messages.allPreds(13L) shouldBe Set(12L)
    Messages.allPreds(14L) shouldBe Set(11L, 12L)
    Messages.allPreds(15L) shouldBe Set(11L, 12L, 13L)
  }

  "enabled" should "correctly return whether a message is enabled" in {
    Messages.isEnabled(11L) shouldBe true
    Messages.isEnabled(12L) shouldBe false
    Messages.isEnabled(13L) shouldBe false
    Messages.isEnabled(14L) shouldBe false
    Messages.isEnabled(15L) shouldBe false
  }

  "before" should "correctly return whether a message is causally before another" in {
    Messages.isBefore(11L, 11L) shouldBe false
    Messages.isBefore(11L, 12L) shouldBe false

    Messages.isBefore(12L, 13L) shouldBe true
    Messages.isBefore(13L, 12L) shouldBe false

    Messages.isBefore(14L, 13L) shouldBe false
    Messages.isBefore(14L, 11L) shouldBe false
    Messages.isBefore(14L, 12L) shouldBe false
    Messages.isBefore(12L, 14L) shouldBe true
    Messages.isBefore(11L, 14L) shouldBe true

    Messages.isBefore(15L, 11L) shouldBe false
    Messages.isBefore(11L, 15L) shouldBe true
    Messages.isBefore(12L, 15L) shouldBe true
    Messages.isBefore(13L, 15L) shouldBe true
  }
}
