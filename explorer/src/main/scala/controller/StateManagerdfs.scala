package controller

import domain.Configuration
import explorer.protocol.{MessageId, ProgramEvent}

import scala.collection.mutable

// define the search heuristic here
class StateManagerdfs(val depth: Int = 1) extends StateManager {


  private val configurationManager = new ConfigurationManager(depth)

  private val fringe: mutable.ArrayStack[Configuration] = mutable.ArrayStack(configurationManager.getInitialConf)

  def scheduleNextMessage: Option[MessageId] = {
    configurationManager.getNextMessage(fringe.top)
  }

  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    val newConfs: List[Configuration] = configurationManager.getNext(fringe.top, events, predecessors)

    assert(newConfs.size <= depth+1)
    fringe.foreach(c=> println(c.toString))
    fringe.pop()
    newConfs.foreach(c => fringe.push(c))

  }

}
