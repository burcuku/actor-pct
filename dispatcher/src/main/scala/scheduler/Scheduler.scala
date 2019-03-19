package scheduler

import scheduler.Scheduler.MessageId

trait Scheduler {
  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getNumScheduledMsgs: Int
}

object Scheduler {
  type MessageId = Long
  type ChainId = Long

  case class Message(id: MessageId, preds: Set[MessageId], var received: Boolean = false)
}

trait SchedulerOptions

object NOOptions extends SchedulerOptions
