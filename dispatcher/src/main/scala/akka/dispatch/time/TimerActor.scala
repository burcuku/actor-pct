package akka.dispatch.time

import akka.actor.Actor
import akka.dispatch.time.TimerActor.AdvanceTime

import scala.concurrent.duration.FiniteDuration


class TimerActor(step: FiniteDuration) extends Actor {
  override def receive: Receive = {
    case AdvanceTime =>
      MockTime.time.advance(step)
      self ! AdvanceTime
    case _ => // do nth
  }
}

object TimerActor {
  case object AdvanceTime
}
