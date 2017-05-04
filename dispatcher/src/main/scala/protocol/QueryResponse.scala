package protocol

sealed abstract class QueryResponse
case class ActionResponse(events: List[Event]) extends QueryResponse
case class State(actorId: String, behavior: StateColor, vars: String)
case class StateColor(r: Float, g: Float, b: Float, a: Float)

object QueryResponses {
  val ACTION_RESPONSE = "ACTION_RESPONSE"
}


