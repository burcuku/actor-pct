package controller

import akka.dispatch.ProgramEvent
import domain.Configuration
import protocol.MessageId

import scala.collection.mutable

// define the search heuristic here
class StateManagerdfs extends StateManager {


  private val configurationManager = new ConfigurationManager()

  private val fringe: mutable.ArrayStack[Configuration] = mutable.ArrayStack(configurationManager.getInitialConf)

  def scheduleNextMessage: Option[MessageId] = {
    configurationManager.getNextMessage(fringe.top)
  }

  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    val newConfs: List[Configuration] = configurationManager.getNext(fringe.top, events, predecessors)

    fringe.pop()

    for(c <- newConfs) {
      fringe.push(c)
    }

  }



}
