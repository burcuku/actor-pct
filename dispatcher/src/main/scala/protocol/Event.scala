package protocol

import akka.actor.ActorRef
import akka.dispatch.Envelope

sealed abstract class Event

case class ActorCreated(actor: ActorRef) extends Event
case class ActorDestroyed(actor: ActorRef) extends Event

case class MessageSent(receiver: ActorRef, msg: Envelope) extends Event
case class MessageReceived(receiver: ActorRef, msg: Envelope) extends Event
case class MessageDropped(receiver: ActorRef, msg: Envelope) extends Event

object Events {
  val ACTOR_CREATED = "ACTOR_CREATED"
  val ACTOR_DESTROYED = "ACTOR_DESTROYED"
  val MESSAGE_SENT = "MESSAGE_SENT"
  val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
  val MESSAGE_DROPPED = "MESSAGE_DROPPED"
}
