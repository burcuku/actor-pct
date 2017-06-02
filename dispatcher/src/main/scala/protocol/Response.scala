package protocol

sealed abstract class Response
case class ActionResponse(events: List[Event]) extends Response


