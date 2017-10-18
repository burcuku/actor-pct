package pct

trait PCTScheduler {
  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getPrioInvPoints: List[Int]
  def getNumScheduledMsgs: Int
  def getNumChains: Int
  def getMaxNumAvailableChains: Int
  def getChainsOfMsgs: List[List[MessageId]]
}
