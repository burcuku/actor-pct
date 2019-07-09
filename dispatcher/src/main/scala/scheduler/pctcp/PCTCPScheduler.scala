package scheduler.pctcp

import akka.dispatch.InternalProgramEvent
import explorer.protocol.MessageId
import scheduler.Scheduler

trait PCTCPScheduler extends Scheduler {
  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit
  def scheduleNextMessage: Option[MessageId]

  def getSchedule: List[MessageId]
  def getPrioInvPoints: List[Int]
  def getNumScheduledMsgs: Int
  def getNumChains: Int
  def getMaxNumAvailableChains: Int
  def getChainsOfMsgs: List[List[MessageId]]
}
