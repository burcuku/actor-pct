package controller

import domain.Configuration
import explorer.protocol.{MessageId, ProgramEvent}


// Manage caching here, probably
class ConfigurationManager(val depth: Int) {

  val cacheManager: CacheManager = new CacheManager()

//  val eventManager: Map[String, Int] = Map[String, Int]()

  def getInitialConf: Configuration = {
    val initConf: Configuration = Configuration() //todo
    initConf.makeInit(0, "Init Message")
    initConf
  }

  def getNextMessage(configuration: Configuration): Option[MessageId]= {
    if (cacheManager.isExplored(configuration))
      Option.empty[MessageId]
    else
      configuration.schedule()
  }

  def getNext(parent: Configuration, events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): List[Configuration] = {

    val children : List[Configuration]  =  parent.getChildren(events, predecessors, depth)
    cacheManager.track(parent, children)
    return children
  }
}
