import scheduler.SchedulerOptions

package object pct {
  case class PCTOptions(randomSeed: Long = System.currentTimeMillis(),
                        maxMessages: Int = 0,
                        bugDepth: Int = 1,
                        alg: String = "AG") extends SchedulerOptions {
    override def toString: String = ("Random Seed: " + randomSeed + "\n" + "Max # of Messages: " + maxMessages + "\n"
      + "Bug depth: " + bugDepth + "\n" + "Algorithm: " + alg)
  }

  case class RandomWalkOptions(randomSeed: Long = System.currentTimeMillis()) extends SchedulerOptions {
    override def toString: String = "Random Seed: " + randomSeed + "\n"
  }
}
