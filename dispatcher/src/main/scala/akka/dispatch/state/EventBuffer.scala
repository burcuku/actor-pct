package akka.dispatch.state

import akka.dispatch.ProgramEvent
import akka.dispatch.state.Messages.MessageId

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
  * Keeps the list of actor events generated in the execution of the program
  * Note: Accessed by the dispatcher thread to:
  *    - insert of an action event (ACTOR_CREATED, MESSAGE_SENT, etc.)
  *    - insert of a log event (e.g. when an Exception occurs)
  *    - read/consume in response to a user request
  *    (send initial list of events, after dispatch completes or the program terminates)
  */
class EventBuffer {

  /**
    * The list of events occurred in the actor program
    */
  private val events: ListBuffer[(MessageId, ProgramEvent)] = ListBuffer()

  def isEmpty: Boolean = events.isEmpty

  def addEvent(id: MessageId, e: ProgramEvent): Unit = events += ((id, e))

  def addEvent(e: ProgramEvent): Unit = events += ((NO_MSG, e))

  def clear(): Unit = events.clear()

  def getAllEvents: List[ProgramEvent] = events.map(_._2).toList

  /**
    * Returns the list of events of a particular event type
    */
  def getEvents[T <: ProgramEvent : ClassTag]: List[(MessageId, ProgramEvent)] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    events.toList.filter(e => clazz.isInstance(e._2))
  }

  /**
    * The index of (newly added) events to be sent to the debugger
    */
  private var recent = 0

  /**
    * Returns the list of events added after last query
    */
  def consumeEvents: List[(MessageId, ProgramEvent)] = {
    val list = events.toList.drop(recent)
    //events.clear()
    recent = events.size
    list
  }

  override def toString: String = events.toList.toString

  private val NO_MSG = -1
}