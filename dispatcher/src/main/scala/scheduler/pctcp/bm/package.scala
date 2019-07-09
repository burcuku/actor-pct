package scheduler.pctcp

import explorer.protocol.MessageId

package object bm {
  case class Message(id: MessageId, preds: Set[MessageId], var received: Boolean = false)
}
