package object protocol {
  type MessageId = Long

  case class Message(id: MessageId, preds: Set[MessageId], var received: Boolean = false)
}
