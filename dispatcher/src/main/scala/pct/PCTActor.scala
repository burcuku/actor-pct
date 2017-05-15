package pct

import akka.actor.{Actor, Props}
import akka.dispatch.PCTDispatcher
import protocol.{ActionResponse, Event}

class PCTActor extends Actor {
  //todo fill in the class

  override def receive: Receive = {
    case response: ActionResponse =>  println("PCTActor received: " + response.events)
  }

  // Note: To deliver message to the selected actor with actorId "actorId", call:
  // PCTDispatcher.dispatchToActor(actorId) // asynchronously commands the Dispatcher to dispatch to this actor
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
