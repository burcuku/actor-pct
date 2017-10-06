package object pct {

  type MessageId = Long
  type ChainId = Long

  case class Message(id: MessageId, preds: Set[MessageId], var received: Boolean = false)

  case class PCTOptions(randomSeed: Long = System.currentTimeMillis(),
                        maxMessages: Int = 0,
                        bugDepth: Int = 1) {
    override def toString: String = "Random Seed: " + randomSeed + "\n" + "Max # of Messages: " + maxMessages + "\n" + "Bug depth: " + bugDepth
  }
}
