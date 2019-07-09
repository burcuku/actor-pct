package controller

import domain.Configuration
import explorer.protocol.{MessageId, ProgramEvent}

import scala.collection.mutable.ListBuffer

// Manage caching here, probably
class ConfigurationManager {

  val cacheManager: CacheManager = new CacheManager()

  def getInitialConf: Configuration = {
    Configuration() //todo
  }

  def getNextMessage(configuration: Configuration): Option[MessageId]= {
    if (cacheManager.isExplored(configuration))
      Option.empty[MessageId]
    else
      configuration.schedule()
  }

  def getNext(parent: Configuration, events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): List[Configuration] = {

    val children: ListBuffer[Configuration] = new ListBuffer[Configuration]()

    children.prepend(new Configuration(parent, events, predecessors))

    cacheManager.track(parent, children.toList)

    for(i <- 0 to parent.orderedEvents.size) {
      children.prepend(new Configuration(parent, i))
    }

    children.toList

  }
}
