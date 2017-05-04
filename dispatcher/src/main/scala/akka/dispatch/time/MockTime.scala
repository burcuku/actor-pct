package akka.dispatch.time

import akka.actor.{ActorRef, Cancellable, NoSerializationVerificationNeeded}
import com.miguno.akka.testing.VirtualTime

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object MockTime {

  val time = new VirtualTime

  def schedule(initialDelay: FiniteDuration, interval: FiniteDuration, receiver: ActorRef, msg: Any)(implicit executor: ExecutionContext): Cancellable =
    time.scheduler.schedule(initialDelay, interval, receiver, msg)

  // for now, we use a single map for all actors
  private val timers = mutable.Map[String, Timer]()

  case class Timer(name: String, msg: Any, repeat: Boolean)(implicit executor: ExecutionContext)
    extends NoSerializationVerificationNeeded {
    private var ref: Option[Cancellable] = _

    def schedule(actor: ActorRef, timeout: FiniteDuration): Unit =
      ref = Some(
        if (repeat) time.scheduler.schedule(timeout, timeout, actor, msg)
        else time.scheduler.scheduleOnce(timeout, actor, msg))

    def cancel(): Unit =
      if (ref.isDefined) {
        ref.get.cancel()
        ref = None
      }
  }

  def setTimer(self: ActorRef, name: String, msg: Any, timeout: FiniteDuration, repeat: Boolean = false)(implicit executor: ExecutionContext): Unit = {
    if(timers.contains(name))
      timers(name).cancel

    val timer = Timer(name, msg, repeat)
    timer.schedule(self, timeout)
    timers.put(name, timer)
  }

  def cancelTimer(self: ActorRef, name: String): Unit = {
    if(timers.contains(name))
      timers(name).cancel
    timers -= name
  }

}
