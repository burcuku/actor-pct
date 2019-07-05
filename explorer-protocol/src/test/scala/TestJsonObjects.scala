import debugger.protocol._

object TestJsonObjects extends App {

  import debugger.protocol.QueryResponses._

  val receiveResponse = ActionResponse(List(ActorCreated("createdActor1"), ActorCreated("createdActor2"), ActorDestroyed("destroyedActor1")),
    List(State("actor1", StateColor(255, 0, 0 ,0), "values1"), State("actor2", StateColor(0, 0, 255 ,0), "values2")))

  val receiveJson = QueryResponseJsonFormat.write(receiveResponse)
  println(receiveJson)

  println(QueryResponseJsonFormat.read(receiveJson))
}
