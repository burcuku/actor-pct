package scheduler.rapos

import akka.actor.ActorSystem
import akka.dispatch.SchedulingStrategy
import protocol.Response
import scheduler.SchedulerOptions

class RaposStrategy(pctOptions: SchedulerOptions) extends SchedulingStrategy {

  override def setUp(system: ActorSystem): Unit = ???

  def putResponse(response: Response): Unit = ???
}