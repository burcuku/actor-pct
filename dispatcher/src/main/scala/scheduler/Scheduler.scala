package scheduler

import akka.dispatch.ProgramEvent
import protocol.MessageId

trait Scheduler {
  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getNumScheduledMsgs: Int
}

trait SchedulerOptions

object NOOptions extends SchedulerOptions
