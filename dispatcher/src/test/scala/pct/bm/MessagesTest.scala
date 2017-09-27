package pct.bm

import org.scalatest.{FlatSpec, Matchers}
import pct.Message

class MessagesTest extends FlatSpec with Matchers {

  val message1 = Message(11L, Set())
  val message2 = Message(12L, Set(), received = true)
  val message3 = Message(13L, Set(12L), received = true)
  val message4 = Message(14L, Set(11L, 12L))
  val message5 = Message(15L, Set(11L, 13L))

  val messages = new Messages()
  List(message1, message2, message3, message4, message5).foreach(m => messages.putMessage(m))

  "all preds" should "correctly return predecessors" in {
    messages.allPreds(11L) shouldBe Set()
    messages.allPreds(13L) shouldBe Set(12L)
    messages.allPreds(14L) shouldBe Set(11L, 12L)
    messages.allPreds(15L) shouldBe Set(11L, 12L, 13L)
  }

  "enabled" should "correctly return whether a message is enabled" in {
    messages.isEnabled(11L) shouldBe true
    messages.isEnabled(12L) shouldBe false
    messages.isEnabled(13L) shouldBe false
    messages.isEnabled(14L) shouldBe false
    messages.isEnabled(15L) shouldBe false
  }

  "before" should "correctly return whether a message is causally before another" in {
    messages.isBefore(11L, 11L) shouldBe false
    messages.isBefore(11L, 12L) shouldBe false

    messages.isBefore(12L, 13L) shouldBe true
    messages.isBefore(13L, 12L) shouldBe false

    messages.isBefore(14L, 13L) shouldBe false
    messages.isBefore(14L, 11L) shouldBe false
    messages.isBefore(14L, 12L) shouldBe false
    messages.isBefore(12L, 14L) shouldBe true
    messages.isBefore(11L, 14L) shouldBe true

    messages.isBefore(15L, 11L) shouldBe false
    messages.isBefore(11L, 15L) shouldBe true
    messages.isBefore(12L, 15L) shouldBe true
    messages.isBefore(13L, 15L) shouldBe true
  }
}
