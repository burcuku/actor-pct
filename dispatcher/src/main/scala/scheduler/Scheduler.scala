package scheduler

import protocol.MessageId

trait Scheduler {
  def addNewMessages(predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getNumScheduledMsgs: Int
}

object Scheduler {

  type ChainId = Long


}

trait SchedulerOptions

object NOOptions extends SchedulerOptions
