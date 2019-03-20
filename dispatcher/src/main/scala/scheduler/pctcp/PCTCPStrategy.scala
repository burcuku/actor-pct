package scheduler.pctcp

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{DispatcherInterface, DispatcherOptions, SchedulingStrategy}
import pctcp.PCTCPOptions
import protocol.{InitRequest, Response}
import scheduler.{NOOptions, SchedulerOptions}

class PCTCPStrategy(options: SchedulerOptions) extends SchedulingStrategy {
  var pctcpActor: Option[ActorRef] = None

  val pctcpOptions: PCTCPOptions = if(!options.equals(NOOptions)) options.asInstanceOf[PCTCPOptions]
    else  PCTCPOptions(DispatcherOptions.randomSeed, DispatcherOptions.maxMessages, DispatcherOptions.bugDepth)

  override def setUp(system: ActorSystem): Unit = {
    pctcpActor = Some(system.actorOf(PCTCPActor.props(pctcpOptions), "PCTCPActor"))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = pctcpActor match {
    case Some(actor) => actor ! response
    case None => println("The actor for PCT algorithm is not created.")
  }
}
