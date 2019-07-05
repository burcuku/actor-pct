package scheduler

package object rapos {
  case class RaposOptions(randomSeed: Long = System.currentTimeMillis()) extends SchedulerOptions {
    override def toString: String = "Random Seed: " + randomSeed + "\n"
  }
}
