package pct

import scala.util.Random

/*
class PCTStrategy(options: PCTOptions) {
  private var msgIndex: Int = 0
  private val randInt = new Random(options.randomSeed)

  private val decomposition = new PCTDecomposition(options)

  private val prioInvPoints: List[Int] = (0 until options.bugDepth)
    .map(i => randInt.nextInt(options.maxMessages))
    .toList

  def getNextMessage: Message = {
    val nextMsg = decomposition.getMinEnabledMessage()
    if (prioInvPoints.contains(msgIndex))
      decomposition.decreasePriority(nextMsg.chain.get)
    msgIndex += 1
    nextMsg
  }
}*/
