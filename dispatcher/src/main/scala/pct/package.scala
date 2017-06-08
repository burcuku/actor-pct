package object pct {

  type MessageId = Long

  case class Message(id: MessageId, preds: Set[MessageId], var received: Boolean = false)

  case class PCTOptions(randomSeed: Long = System.currentTimeMillis(),
                        maxChains: Int = 0,
                        maxMessages: Int = 0,
                        bugDepth: Int = 1)

}
