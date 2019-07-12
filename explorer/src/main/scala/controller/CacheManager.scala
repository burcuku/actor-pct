package controller

import domain.Configuration

import scala.collection.mutable

class CacheManager {

  private val activeChildCount: mutable.Map[Configuration, Int] = mutable.Map()

  private val parentConf: mutable.Map[Configuration, Configuration] = mutable.Map()

  private def propagateAndStore(configuration: Configuration): Unit = {

    if(configuration.isMaximal){
      //todo: store configuration depending on caching structure
    }

    val parent: Configuration = parentConf(configuration)
    activeChildCount(parent)-=1
    if(activeChildCount(parent) == 0)
      propagateAndStore(parent)

  }

  def isExplored(configuration: Configuration): Boolean = {
    if(configuration.isFinal) {
      propagateAndStore(configuration)
      return true
    }
    else {
      //todo: check if it is in one of the stores
      false
    }
  }

  def track(parent: Configuration, children: List[Configuration]): Unit = {
    activeChildCount+= (parent -> children.size)

    for (c<- children) {
      parentConf+= (c-> parent)
    }
  }
}
