package akka.dispatch.state

import akka.actor.{ActorRef, Cell}
import akka.dispatch.state.Messages.MessageId

import scala.collection.{Set, mutable}

class ActorMessagesMap(messages: Messages) {

  private var actorMessages = mutable.HashMap[Cell, List[MessageId]]()

  def numActors: Int = actorMessages.keySet.size

  def addMessage(actor: Cell, msg: MessageId): Unit = {
    val msgs: List[MessageId] = actorMessages.getOrElse(actor, List[MessageId]())
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

  def getAllMessages(actor: Cell): Option[List[MessageId]] = actorMessages.get(actor)

  def getMessage(receiverId: String, senderId: String, message: Any): Option[MessageId] = getActor(receiverId) match {
    case Some(receiver) => getMessage(receiver, senderId, message)
    case None => throw new Exception("ActorMessagesMap - No such receiver actor")
  }

  def getMessage(receiver: Cell, senderId: String, message: Any): Option[MessageId] = {
    def getMessage(list: List[MessageId]): Option[MessageId] = list match {
      case x :: xs if messages.get(x).envelope.sender.path.toString.equals(senderId) && messages.get(x).envelope.message.equals(message) => Some(x)
      case x :: xs => getMessage(xs)
      case Nil => None
    }
    getMessage(actorMessages(receiver))
  }

  def removeMessage(receiverId: String, senderId: String, message: Any): Option[MessageId] = getActor(receiverId) match {
    case Some(receiver) => removeMessage(receiver, senderId, message)
    case None => throw new Exception("ActorMessagesMap - No such receiver actor")
  }

  def removeMessage(receiver: Cell, senderId: String, message: Any): Option[MessageId] = {
    def removeMessage(list: List[MessageId], acc: List[MessageId]): (Option[MessageId], List[MessageId]) = list match {
      case x :: xs if messages.get(x).envelope.sender.path.toString.equals(senderId) && messages.get(x).envelope.message.equals(message) => (Some(x), acc ++ xs)
      case x :: xs => removeMessage(xs, acc :+ x)
      case Nil => (None, acc)
    }

    val (envelope, newList) = removeMessage(actorMessages(receiver), Nil)
    actorMessages += (receiver -> newList)
    envelope
  }

  def addActor(actor: Cell): Any = {
    actorMessages.get(actor) match {
      case None => actorMessages += (actor -> List())
      case Some(msgList) => System.err.println("ActorMessagesMap - Message received before actor added into the map")
    }
  }

  def removeActor(actor: Cell): Any = {
    actorMessages.get(actor) match {
      case Some(msgList) =>   actorMessages -= actor
      // the case for the utility actors not added onto the map
      case None => //System.err.println("ActorMessagesMap - Terminated an actor that does not exist in the map: " + actor.self)
    }
  }

  def removeHeadMessage(actor: Cell): Option[MessageId] = {
    actorMessages.get(actor) match {
      case None | Some(Nil) => None
      case Some(msgList) =>
        actorMessages += (actor -> msgList.tail)
        Some(msgList.head)
    }
  }



  def hasActor(actorId: ActorRef): Option[Cell] = {
    def helper(actors: List[Cell]): Option[Cell] = actors match {
      case Nil => None
      case x :: xs if x.self.equals(actorId) => Some(x)
      case x :: xs => helper(xs)
    }
    helper(actorMessages.keySet.toList)
  }

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    */
  def isAllEmpty: Boolean = actorMessages.keys.forall(isEmpty)

  /**
    * @return true if the given actor has an empty message list
    */
  def isEmpty(a: Cell): Boolean = actorMessages.get(a) match {
    case Some(list) => list.isEmpty
    case None => true // must not hit here
  }

  def toMapWithActorRef: mutable.HashMap[ActorRef, List[MessageId]] = {
    actorMessages.map(a => (a._1.self, a._2)).clone()
  }

  def toList: List[(Cell, List[MessageId])] = {
    actorMessages.toList
  }

  def toListWithActorCell: List[(Cell, List[MessageId])] = {
    var list: List[(Cell, List[MessageId])] = List()
    actorMessages.foreach(a => list = list :+ a)
    list.sortBy(_._1.self.toString()) // costly but easier to manage user input
  }

  def toListWithActorRef: List[(ActorRef, List[MessageId])] = {
    var list: List[(ActorRef, List[MessageId])] = List()
    actorMessages.foreach(a => list = list :+ (a._1.self, a._2))
    list.sortBy(_._1.toString()) // costly but easier to manage user input
  }

  def toListWithActorPath: List[(String, List[MessageId])] = {
    var list: List[(String, List[MessageId])] = List()
    actorMessages.foreach(a => list = list :+ (a._1.self.path.toString, a._2))
    list.sortBy(_._1.toString()) // costly but easier to manage user input
  }
}
