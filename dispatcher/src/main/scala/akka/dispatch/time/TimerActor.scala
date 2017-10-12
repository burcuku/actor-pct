package akka.dispatch.time

import akka.actor.Actor
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.util.CmdLineUtils

import scala.concurrent.duration.FiniteDuration


class TimerActor(step: FiniteDuration, maxCount: Int) extends Actor {
  var count = 0
  override def receive: Receive = {
    case AdvanceTime if count < maxCount =>
      MockTime.time.advance(step)
      count = count + 1
      self ! AdvanceTime
    case AdvanceTime =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Max number of time steps reached: " + maxCount)
    case _ => // do nth
  }
}

object TimerActor {
  case object AdvanceTime
}
