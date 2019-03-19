package protocol

import akka.dispatch.ProgramEvent

sealed abstract class Response

case class ErrorResponse(error: String) extends Response

// some schedulers need dependency information, send the program event as well as its id and the predecessors
case class AddedEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) extends Response


