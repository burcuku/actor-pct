package akka.dispatch

import akka.actor.Cell
import explorer.protocol.MessageId

//todo consider an adapter for the protocol

/**
  * These events are generated by the dispatcher
  */
sealed abstract class InternalProgramEvent

case class ActorCreated(actor: Cell) extends InternalProgramEvent
case class ActorDestroyed(actor: Cell) extends InternalProgramEvent

/** A fresh message is sent **/
case class MessageSent(receiver: Cell, msg: Envelope) extends InternalProgramEvent
/** Message with id is received **/
case class MessageReceived(receiver: Cell, id: MessageId, msg: Envelope) extends InternalProgramEvent
/** Message with id is dropped **/
case class MessageDropped(receiver: Cell, id: MessageId, msg: Envelope) extends InternalProgramEvent

/** As the initial event to be sent and also to be used in tests **/
case object DummyInternalProgramEvent extends InternalProgramEvent

object InternalProgramEvent {
  def areRacyEvents(e1: InternalProgramEvent, e2: InternalProgramEvent): Boolean = !e1.equals(e2) && getActor(e1) == getActor(e2)

  def getActor(e: InternalProgramEvent): Cell = e match {
    case e: ActorCreated =>  e.asInstanceOf[ActorCreated].actor
    case e: ActorDestroyed =>  e.asInstanceOf[ActorDestroyed].actor
    case e: MessageDropped =>  e.asInstanceOf[MessageDropped].receiver
    case e: MessageReceived =>  e.asInstanceOf[MessageReceived].receiver
    case e: MessageSent =>  e.asInstanceOf[MessageSent].receiver
    case _ =>
      println("Unexpected program event: " + e)
      null
  }

  def getContent(e: InternalProgramEvent): String = e match {
    case e: ActorCreated =>  ""
    case e: ActorDestroyed =>  ""
    case e: MessageDropped =>  e.asInstanceOf[MessageDropped].msg.message.toString
    case e: MessageReceived =>  e.asInstanceOf[MessageReceived].msg.message.toString
    case e: MessageSent =>  e.asInstanceOf[MessageSent].msg.message.toString
    case _ =>
      println("Unexpected program event: " + e)
      ""
  }
}