package protocol


sealed abstract class Response

case class ActionResponse(events: List[Event]) extends Response
case class MessagePredecessors(predecessors: Map[Long, Set[Long]]) extends Response


