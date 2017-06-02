package akka.dispatch.io

import akka.actor.ActorSystem
import protocol._

trait IOProvider {
  def setUp(system: ActorSystem): Unit
  def putResponse(response: Response)
}

object NopIOProvider extends IOProvider {
  override def setUp(system: ActorSystem): Unit = {}

  override def putResponse(response: Response): Unit = {}
}

