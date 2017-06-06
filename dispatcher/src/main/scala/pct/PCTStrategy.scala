package pct

import scala.util.Random


class PCTStrategy(options: PCTOptions) {
  private var msgIndex: Int = 0
  private val randInt = new Random //new Random(options.randomSeed)

  private val decomposition = new PCTDecomposition(options)

  private val prioInvPoints: List[Int] = (0 until options.bugDepth)
    .map(i => randInt.nextInt(options.maxMessages))
    .toList
  
  def setNewMessages(ids: List[MessageId]) = {
    decomposition.extend(ids)
    decomposition.minimizeChains
    decomposition.shuffleChains
  }
  
  def getNextMessage: Option[MessageId] = {
    val nextId = decomposition.getMinEnabledMessage()
    nextId match {
      case Some(id) =>
        if (prioInvPoints.contains(msgIndex))
          decomposition.decreasePriority(id)
        msgIndex += 1   
      case _ =>
    }
    nextId
  }
}
