package protocol

import akka.dispatch.ProgramEvent


sealed abstract class Response

case class ErrorResponse(error: String) extends Response
case class MessagePredecessors(predecessors: Map[Long, Set[Long]]) extends Response


