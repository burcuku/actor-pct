package akka.dispatch.io

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.RequestForwarder
import pct.PCTActor
import protocol.{InitRequest, Response}

class PCTIOProvider(pctOptions: pct.PCTOptions) extends IOProvider {
  var pctActor: Option[ActorRef] = None

  override def setUp(system: ActorSystem): Unit = {
    pctActor = Some(system.actorOf(PCTActor.props(pctOptions), "PCTActor"))
    RequestForwarder.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    pctActor match {
      case Some(actor) => actor ! response
      case None => println("The actor for PCT algorithm is not created.")
    }
  }
}
