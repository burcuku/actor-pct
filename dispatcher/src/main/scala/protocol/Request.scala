package protocol

import scheduler.Scheduler.MessageId

trait Request

case class DispatchMessageRequest(messageId: MessageId) extends Request
case class DropMessageRequest(messageId: MessageId) extends Request
case object InitRequest extends Request
case object TerminateRequest extends Request

