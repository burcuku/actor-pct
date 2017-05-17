package protocol

import akka.dispatch.Envelope

sealed abstract class Event

case class ActorCreated(actorId: String) extends Event

case class ActorDestroyed(actorId: String) extends Event

case class MessageSent(receiverId: String, senderId: String, msg: Any) extends Event

case class MessageReceived(receiverId: String, senderId: String, msg: Any) extends Event

case class MessageDropped(receiverId: String, senderId: String, msg: Any) extends Event

case class Log(logType: Int, text: String) extends Event

object Events {
  val ACTOR_CREATED = "ACTOR_CREATED"
  val ACTOR_DESTROYED = "ACTOR_DESTROYED"
  val MESSAGE_SENT = "MESSAGE_SENT"
  val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
  val MESSAGE_DROPPED = "MESSAGE_DROPPED"
  val LOG = "LOG"

  val LOG_DEBUG = 0
  val LOG_INFO = 1
  val LOG_WARNING = 2
  val LOG_ERROR = 3
}
