package scheduler.pctcp

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{DispatcherInterface, DispatcherOptions}
import pct.PCTOptions
import protocol.{InitRequest, Response}
import scheduler.{NOOptions, SchedulerOptions, SchedulingStrategy}

class PCTCPStrategy(options: SchedulerOptions) extends SchedulingStrategy {
  var pctActor: Option[ActorRef] = None

  val pctOptions: PCTOptions = if(!options.equals(NOOptions)) options.asInstanceOf[PCTOptions]
    else  PCTOptions(DispatcherOptions.randomSeed, DispatcherOptions.maxMessages, DispatcherOptions.bugDepth)

  override def setUp(system: ActorSystem): Unit = {
    pctActor = Some(system.actorOf(PCTCPActor.props(pctOptions), "PCTActor"))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    pctActor match {
      case Some(actor) => actor ! response
      case None => println("The actor for PCT algorithm is not created.")
    }
  }
}
