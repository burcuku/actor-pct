package akka.dispatch

import protocol.Event

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
object ProgramEvents {

  /**
    * The list of events occurred in the actor program
    */
  private val events: ListBuffer[protocol.Event] = ListBuffer()
  /**
    * The index of (newly added) events to be sent to the debugger
    */
  private var recent = 0

  def isEmpty: Boolean = events.isEmpty

  def addEvent(e: Event): ListBuffer[Event] = events += e

  def clear(): Unit = events.clear()

  def getAllEvents: List[Event] = events.toList

  /**
    * Returns the list of events of a particular event type
    */
  def getEvents[T <: Event : ClassTag]: List[Event] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    events.toList.filter(e => clazz.isInstance(e))
  }

   /**
    * Returns the list of events added after last query
    * @return
    */
  def consumeEvents: List[Event] = {
    val list = events.toList.drop(recent)
    //events.clear()
    recent = events.size
    list
  }

  override def toString: String = events.toList.toString

}
