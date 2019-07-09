package scheduler.pos

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.util.CmdLineUtils
import akka.dispatch.{DispatcherInterface, DispatcherOptions, SchedulingStrategy}
import explorer.protocol.{InitRequest, Response}
import scheduler.{NOOptions, SchedulerOptions}

class POSStrategy(options: SchedulerOptions) extends SchedulingStrategy {
  var posActor: Option[ActorRef] = None
  val schedulerOptions: POSOptions = if(!options.equals(NOOptions)) options.asInstanceOf[POSOptions]
  else  POSOptions(DispatcherOptions.randomSeed)

  def setUp(system: ActorSystem): Unit = {
    posActor = Some(system.actorOf(POSActor.props(schedulerOptions.asInstanceOf[POSOptions])))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    posActor match {
      case Some(actor) => actor ! response
      case None => CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "The actor for selecting random messages is not created.")
    }
  }
}