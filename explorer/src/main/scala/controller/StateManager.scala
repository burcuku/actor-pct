package controller

import akka.dispatch.ProgramEvent
import protocol.MessageId


trait StateManager {

  def scheduleNextMessage: Option[MessageId]
  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit


}
