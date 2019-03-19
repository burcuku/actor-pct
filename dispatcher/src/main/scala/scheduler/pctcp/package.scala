import scheduler.SchedulerOptions

package object pctcp {
  type ChainId = Long

  case class PCTCPOptions(randomSeed: Long = System.currentTimeMillis(),
                          maxMessages: Int = 0,
                          bugDepth: Int = 1,
                          alg: String = "AG") extends SchedulerOptions {
    override def toString: String = ("Random Seed: " + randomSeed + "\n" + "Max # of Messages: " + maxMessages + "\n"
      + "Bug depth: " + bugDepth + "\n" + "Algorithm: " + alg)
  }

  case class TaPCTCPOptions(randomSeed: Long = System.currentTimeMillis(),
                            maxRacyMessages: Int = 0,
                            bugDepth: Int = 1,
                            racyMessages: List[String] = List(),
                            alg: String = "AG") extends SchedulerOptions {
    override def toString: String = ("Random Seed: " + randomSeed + "\n" + "Max # of Messages: " + maxRacyMessages + "\n"
      + "Bug depth: " + bugDepth + "\n" + "Algorithm: " + alg)
  }
}
