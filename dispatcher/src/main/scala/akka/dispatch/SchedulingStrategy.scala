package akka.dispatch

import akka.actor.ActorSystem
import explorer.protocol.Response

trait SchedulingStrategy {
  def setUp(system: ActorSystem): Unit
  def putResponse(response: Response)
}

object NopStrategy extends SchedulingStrategy {
  override def setUp(system: ActorSystem): Unit = {}

  override def putResponse(response: Response): Unit = {}
}

