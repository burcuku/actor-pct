package pct

trait PCTScheduler {
  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]
  def getSchedule: List[MessageId]
}
