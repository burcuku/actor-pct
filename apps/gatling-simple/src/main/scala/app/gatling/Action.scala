package app.gatling

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import app.gatling.Action.Execute
import app.gatling.Terminator.ActionDone
import app.gatling.Writer.Write

class Action(name: String, terminator: ActorRef, writer: ActorRef) extends Actor with ActorLogging{

  def receive = {
    case Execute =>
      log.info("Terminator received ActionDone message")
      writer ! Write(name)
      terminator ! ActionDone
  }
}

object Action {
  def props(name: String, terminator: ActorRef, writer: ActorRef) = Props(new Action(name, terminator, writer))
  case object Execute
}
