package controller

import explorer.protocol.{MessageId, ProgramEvent}


trait StateManager {

  def scheduleNextMessage: Option[MessageId]
  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit


}
