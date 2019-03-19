package scheduler.random

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.dispatch.{DispatcherInterface, DispatcherOptions}
import pct.RandomWalkOptions
import protocol._
import scheduler.{NOOptions, SchedulerOptions, SchedulingStrategy}

/**
  * Selects the next message randomly from the set of available messages
  */
class RandomWalkStrategy(options: SchedulerOptions) extends SchedulingStrategy {
  var randomExecActor: Option[ActorRef] = None
  val schedulerOptions: RandomWalkOptions = if(!options.equals(NOOptions)) options.asInstanceOf[RandomWalkOptions]
  else  RandomWalkOptions(DispatcherOptions.randomSeed)

  def setUp(system: ActorSystem): Unit = {
    randomExecActor = Some(system.actorOf(RandomWalkActor.props(schedulerOptions.asInstanceOf[RandomWalkOptions])))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    randomExecActor match {
      case Some(actor) => actor ! response
      case None => println("The actor for selecting random messages is not created.")
    }
  }
}


