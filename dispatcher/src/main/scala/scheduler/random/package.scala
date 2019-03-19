package scheduler

package object random {
  case class RandomWalkOptions(randomSeed: Long = System.currentTimeMillis()) extends SchedulerOptions {
    override def toString: String = "Random Seed: " + randomSeed + "\n"
  }
}
