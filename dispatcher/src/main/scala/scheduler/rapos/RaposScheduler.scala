package scheduler.rapos


import akka.dispatch.{MessageSent, InternalProgramEvent}
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.MessageId
import scheduler.Scheduler

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class RaposScheduler(options: RaposOptions) extends Scheduler with LazyLogging {
  private val rand = new Random(options.randomSeed)

  private var newEvents: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]() // new events
  private var scheduled: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]() // scheduled
  private var lastScheduled: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]() // to check for dependencies
  private var toDelay: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]() // enabled but not in scheduled
  private var schedulable: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]() // schedulable

  private var messages: mutable.Map[MessageId, InternalProgramEvent] = new mutable.HashMap[MessageId, InternalProgramEvent]()
  private var processed: List[MessageId] = List(0)
  private var preds: Map[MessageId, Set[MessageId]] = Map()

  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)
  private var maxNumAvailableMsgs = 0 // for stats


  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    predecessors.keySet.foreach(msg => preds = preds + (msg -> predecessors(msg)))

    events.filter(_._2.isInstanceOf[MessageSent]).foreach( e => { //for now, only messages sent are considered
      newEvents.append(e._1)
      messages.put(e._1, e._2)
    })

    val enabledMsgs = messages.keySet.diff(processed.toSet).filter(isEnabled).toList
    if(maxNumAvailableMsgs < enabledMsgs.size) maxNumAvailableMsgs = enabledMsgs.size
  }

  // for now, implemented only for MessageSent events
  def isIndependentToAll(id: MessageId, ids: ListBuffer[MessageId]): Boolean =
    ids.forall(i => messages(i).asInstanceOf[MessageSent].receiver != messages(id).asInstanceOf[MessageSent].receiver)

  def scheduleNextMessage: Option[MessageId] = {
    // if this is the first event to schedule, set schedulable to all events
    if(numScheduled == 0) {
      newEvents.foreach(e => schedulable.append(e))
      newEvents.clear()
    }

    if (scheduled.isEmpty) {
      if(newEvents.isEmpty && schedulable.isEmpty && toDelay.isEmpty) return None

      // add dependent elements into schedulable
      // 1. all events added in the meanwhile are dependent to an element in prev scheduled:
      newEvents.foreach(e => schedulable.append(e))
      newEvents.clear()

      // 2. events in delayed which are dependent to the scheduled one
      val toRemove: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]()
      toDelay.foreach(e =>
        if(!isIndependentToAll(e, lastScheduled))  {
          schedulable.append(e)
          toRemove.append(e)
        })
      toRemove.foreach(e => toDelay.remove(toDelay.indexOf(e)))

      // if schedulable is empty, add a random event
      if(schedulable.isEmpty && toDelay.nonEmpty) {
        val randEvent = rand.nextInt(toDelay.size)
        schedulable.append(toDelay.remove(randEvent))
      }

      // reads an independent subset to schedulable and removes from scheduled
      randIndependentSubset(schedulable).foreach(e => scheduled.append(e))
      schedulable.foreach(e => toDelay.append(e))
      schedulable.clear()

      lastScheduled.clear()
      scheduled.foreach(e => lastScheduled.append(e))
    }

    if(scheduled.isEmpty) return None

    val next = scheduled.remove(0)
    schedule += next
    numScheduled = numScheduled + 1
    Some(next)
  }


  private def randIndependentSubset(all: ListBuffer[MessageId]): ListBuffer[MessageId] =  {
    val list: mutable.ListBuffer[MessageId] = new mutable.ListBuffer[MessageId]()

    val tmpSet = rand.nextInt(all.size)
    list.append(all.remove(tmpSet))

    list.foreach(e => {
      if(isIndependentToAll(e, list) && rand.nextDouble() > 1/2)
        list.append(e)
    })
    list
  }


  private def isIndependent(m: MessageId): Boolean = scheduled.forall(e => !areRacy(e, m))

  private def areRacy(m1: MessageId, m2: MessageId): Boolean = InternalProgramEvent.areRacyEvents(messages(m1), messages(m2))

  private def schedule(eventId: MessageId): MessageId = {
    schedule += eventId
    numScheduled = numScheduled + 1
    eventId
  }

  def isEnabled(id: MessageId): Boolean = preds(id).forall(x => processed.contains(x))

  def getSchedule: List[MessageId] = schedule.toList

  def getMaxNumAvailableMsgs: Int = maxNumAvailableMsgs

  def getNumScheduledMsgs: Int = numScheduled
}


