package scheduler.pctcp

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{DispatcherInterface, DispatcherOptions}
import pctcp.TaPCTCPOptions
import protocol.{InitRequest, Response}
import scheduler.{NOOptions, SchedulerOptions, SchedulingStrategy}

class TaPCTCPStrategy(options: SchedulerOptions) extends SchedulingStrategy {
  var taPCTCPActor: Option[ActorRef] = None

  val taPCTCPOptions: TaPCTCPOptions = if(!options.equals(NOOptions)) options.asInstanceOf[TaPCTCPOptions]
  else TaPCTCPOptions(DispatcherOptions.randomSeed, DispatcherOptions.maxRacyMessages, DispatcherOptions.bugDepth,
    if(DispatcherOptions.racyMessagePattern.isDefined) DispatcherOptions.racyMessagePattern.get else List())

  override def setUp(system: ActorSystem): Unit = {
    taPCTCPActor = Some(system.actorOf(TaPCTCPActor.props(taPCTCPOptions), "TaPCTCPActor"))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    taPCTCPActor match {
      case Some(actor) => actor ! response
      case None => println("The actor for PCT algorithm is not created.")
    }
  }
}