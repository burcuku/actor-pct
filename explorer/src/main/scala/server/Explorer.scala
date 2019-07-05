package server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import explorer.protocol.Initiate

class ExplorerActor(server: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    // will receive Configuration(str: String) and send CommandRequest to the server
    case _ =>  // server ! Initiate
  }
}

object ExplorerActor {
  def props(server: ActorRef) = Props(new ExplorerActor(server))
}