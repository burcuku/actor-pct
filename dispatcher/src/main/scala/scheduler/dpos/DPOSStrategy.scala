package scheduler.dpos

import akka.actor.ActorSystem
import protocol.Response
import scheduler.{SchedulerOptions, SchedulingStrategy}

class DPOSStrategy(pctOptions: SchedulerOptions) extends SchedulingStrategy {

  override def setUp(system: ActorSystem): Unit = ???

  def putResponse(response: Response): Unit = ???
}
