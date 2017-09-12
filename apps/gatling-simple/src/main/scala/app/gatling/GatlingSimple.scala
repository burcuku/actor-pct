package app.gatling

import akka.actor.ActorSystem
import akka.dispatch.PCTDispatcher
import app.gatling.Action.Execute

object GatlingSimple extends App {
  val system = ActorSystem("sys")

  val writer = system.actorOf(Writer.props, "writer")
  val terminator = system.actorOf(Terminator.props(actionNum = 1, writer), "terminator")
  val action = system.actorOf(Action.props("data", terminator, writer), "action")

  action ! Execute

  PCTDispatcher.setActorSystem(system)
  PCTDispatcher.setUp()
  system.awaitTermination()
}

