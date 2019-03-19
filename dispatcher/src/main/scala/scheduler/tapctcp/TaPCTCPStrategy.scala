package scheduler.tapctcp

import akka.actor.ActorSystem
import protocol.Response
import scheduler.{SchedulerOptions, SchedulingStrategy}

class TaPCTCPStrategy(pctOptions: SchedulerOptions) extends SchedulingStrategy {

  override def setUp(system: ActorSystem): Unit = ???

  def putResponse(response: Response): Unit = ???

}
