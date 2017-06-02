package protocol

trait Request

case class DispatchToActorRequest(actorId: String) extends Request
case class DropActorMessageRequest(actorId: String) extends Request
case class DispatchMessageRequest(messageId: Long) extends Request
case class DropMessageRequest(messageId: Long) extends Request
case object InitRequest extends Request
case object TerminateRequest extends Request

