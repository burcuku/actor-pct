package akka.dispatch

import akka.actor.{ActorRef, Cell}

import scala.collection.{Set, mutable}
import scala.util.control.Breaks.{break, breakable}

class ActorMessagesMap {

  private var actorMessages = mutable.HashMap[Cell, List[Envelope]]()

  def numActors: Int = actorMessages.keySet.size

  def addMessage(actor: Cell, msg: Envelope): Unit = {
    val msgs: List[Envelope] = actorMessages.getOrElse(actor, List[Envelope]())
    actorMessages += (actor -> (msgs :+ msg) )
  }

  def getActor(actorId: String): Option[Cell] = {
    def helper(actors: List[Cell]): Option[Cell] = actors match {
      case Nil => None
      case x :: xs if x.self.path.toString.equals(actorId) => Some(x)
      case x :: xs => helper(xs)
    }
    helper(actorMessages.keySet.toList)
  }

  def getActorIdByIndex(no: Int): String = {
    if(numActors < no) {
      throw new Exception("ActorMessagesMap - Actor index requested is out of bounds")
    }
    val sorted = actorMessages.toList.sortBy(_._1.self.toString())
    sorted(no-1)._1.self.toString()
  }

  def getActorNameByIndex(no: Int): String = {
    if(numActors < no) {
      throw new Exception("ActorMessagesMap - Actor index requested is out of bounds")
    }
    val sorted = actorMessages.toList.sortBy(_._1.self.toString())
    sorted(no-1)._1.self.path.toString
  }

  def getAllActors: Set[Cell] = {
    actorMessages.keySet
  }
  
  def addActor(actor: Cell): Any = {
    actorMessages.get(actor) match {
      case None => actorMessages += (actor -> List() )
      case Some(msgList) =>   System.err.println("ActorMessagesMap - Message received before actor added into the map")
    }
  }

  def removeActor(actor: Cell): Any = {
    actorMessages.get(actor) match {
      case Some(msgList) =>   actorMessages -= actor
      // the case for the utility actors not added onto the map
      case None => //System.err.println("ActorMessagesMap - Terminated an actor that does not exist in the map: " + actor.self)
    }
  }

  def removeHeadMessage(actor: Cell): Option[Envelope] = {
    actorMessages.get(actor) match {
      case None | Some(Nil) => None
      case Some(msgList) =>
        actorMessages += (actor -> msgList.tail)
        Some(msgList.head)
    }
  }

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    *         except for the log1-Logging$DefaultLogger actor (no logic processing messages)
    */
  def isAllEmptyExceptLogger: Boolean = {
    def check(keys: Iterable[Cell]): Boolean = {
      for(k <- keys) {
        breakable {
          if (k.self.toString().contains("log1-Logging$DefaultLogger")) break
          if (!isEmpty(k)) return false
        }
      }
      true
    }

    check(actorMessages.keys)
  }

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    */
  def isAllEmpty: Boolean = {
     def check(keys: Iterable[Cell]): Boolean = {
       for(k <- keys) {
         if (!isEmpty(k)) return false
       }
       true
     }

    check(actorMessages.keys)
  }

  /**
    * @return true if the given actor has an empty message list
    */
  def isEmpty(a: Cell): Boolean = actorMessages.get(a) match {
    case Some(list) => list.isEmpty
    case None => true // must not hit here
  }

  def toMapWithActorRef: mutable.HashMap[ActorRef, List[Envelope]] = {
    actorMessages.map(a => (a._1.self, a._2)).clone()
  }

  def toList: List[(Cell, List[Envelope])] = {
    actorMessages.toList
  }

  def toListWithActorCell: List[(Cell, List[Envelope])] = {
    var list: List[(Cell, List[Envelope])] = List()
    actorMessages.foreach(a => list = list :+ a)
    list.sortBy(_._1.self.toString()) // costly but easier to manage user input
  }

  def toListWithActorRef: List[(ActorRef, List[Envelope])] = {
    var list: List[(ActorRef, List[Envelope])] = List()
    actorMessages.foreach(a => list = list :+ (a._1.self, a._2))
    list.sortBy(_._1.toString()) // costly but easier to manage user input
  }

  def toListWithActorPath: List[(String, List[Envelope])] = {
    var list: List[(String, List[Envelope])] = List()
    actorMessages.foreach(a => list = list :+ (a._1.self.path.toString, a._2))
    list.sortBy(_._1.toString()) // costly but easier to manage user input
  }
}
