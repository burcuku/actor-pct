package akka.dispatch

import akka.actor.Cell
import akka.dispatch.state.Messages.MessageId

/**
  * These events are generated by the dispatcher
  */
sealed abstract class ProgramEvent

case class ActorCreated(actor: Cell) extends ProgramEvent
case class ActorDestroyed(actor: Cell) extends ProgramEvent

/** A fresh message is sent **/
case class MessageSent(receiver: Cell, msg: Envelope) extends ProgramEvent
/** Message with id is received **/
case class MessageReceived(receiver: Cell, id: MessageId, msg: Envelope) extends ProgramEvent
/** Message with id is dropped **/
case class MessageDropped(receiver: Cell, id: MessageId, msg: Envelope) extends ProgramEvent

/** As the initial event to be sent and also to be used in tests **/
case object DummyProgramEvent extends ProgramEvent
