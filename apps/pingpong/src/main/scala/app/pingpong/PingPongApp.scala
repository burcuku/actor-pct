package app.pingpong

import akka.actor.{ActorSystem, Props}
import akka.dispatch.PCTDispatcher

object PingPongApp extends App {
  val system = ActorSystem("sys")

  val pongActor1 = system.actorOf(PongActor.props, "pongActor1")
  val pongActor2 = system.actorOf(PongActor.props, "pongActor2")
  val pingActor1 = system.actorOf(Props(new PingActor(pongActor1, 1, false)), name = "pingActor1")
  val pingActor2 = system.actorOf(Props(new PingActor(pongActor2, 2, true)), name = "pingActor2")

  pingActor1 ! PingActor.Initialize
  pingActor2 ! PingActor.Initialize

  PCTDispatcher.setActorSystem(system)
  PCTDispatcher.setUp()
  system.awaitTermination()
}


