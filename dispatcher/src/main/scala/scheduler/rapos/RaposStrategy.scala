package scheduler.rapos

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.util.CmdLineUtils
import akka.dispatch.{DispatcherInterface, DispatcherOptions, SchedulingStrategy}
import explorer.protocol.{InitRequest, Response}
import scheduler.{NOOptions, SchedulerOptions}

class RaposStrategy(raposOptions: SchedulerOptions) extends SchedulingStrategy {

  var posActor: Option[ActorRef] = None
  val schedulerOptions: RaposOptions = if(!raposOptions.equals(NOOptions)) raposOptions.asInstanceOf[RaposOptions]
  else  RaposOptions(DispatcherOptions.randomSeed)

  def setUp(system: ActorSystem): Unit = {
    posActor = Some(system.actorOf(RaposActor.props(schedulerOptions.asInstanceOf[RaposOptions])))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    posActor match {
      case Some(actor) => actor ! response
      case None => CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "The actor for selecting random messages is not created.")
    }
  }
}