package akka.dispatch.util

import java.util.concurrent.atomic.AtomicLong

class IdGenerator(initial: Long = 0L) {
  private val n = new AtomicLong(initial)
  def next: Long = n.getAndIncrement()
}
