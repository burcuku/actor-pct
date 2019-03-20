package scheduler

package object pos {
  case class POSOptions(randomSeed: Long = System.currentTimeMillis()) extends SchedulerOptions {
    override def toString: String = "Random Seed: " + randomSeed + "\n"
  }
  case class DPOSOptions(randomSeed: Long = System.currentTimeMillis(),
                            maxRacyMessages: Int = 0,
                            bugDepth: Int = 1) extends SchedulerOptions {
    override def toString: String = ("Random Seed: " + randomSeed + "\n" + "Max # of Racy Messages: " + maxRacyMessages + "\n"
      + "Bug depth: " + bugDepth)
  }
}
