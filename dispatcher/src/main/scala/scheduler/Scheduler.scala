package scheduler

import akka.dispatch.InternalProgramEvent
import explorer.protocol.MessageId

trait Scheduler {
  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getNumScheduledMsgs: Int
}
