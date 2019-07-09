package scheduler.pos

import akka.dispatch.{MessageSent, InternalProgramEvent}
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.MessageId
import scheduler.Scheduler

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class POSScheduler(options: POSOptions) extends Scheduler with LazyLogging {
  private val rand = new Random(options.randomSeed)

  private var priorityMap: mutable.Map[Double, MessageId] = new mutable.HashMap[Double, MessageId]()

  private var maxNumAvailableMsgs = 0 // for stats
  // for checking the number of concurrently enabled messages
  private var messages: mutable.Map[MessageId, InternalProgramEvent] = new mutable.HashMap[MessageId, InternalProgramEvent]()
  private var processed: List[MessageId] = List(0)
  private var preds: Map[MessageId, Set[MessageId]] = Map()

  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)


  def addNewMessages(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
    predecessors.keySet.foreach(msg => preds = preds + (msg -> predecessors(msg)))

    events.filter(_._2.isInstanceOf[MessageSent]).foreach( e => { //for now, only messages sent are considered
        priorityMap.put(rand.nextDouble(), e._1)
        messages.put(e._1, e._2)
    })

    val enabledMsgs = messages.keySet.diff(processed.toSet).filter(isEnabled).toList
    if(maxNumAvailableMsgs < enabledMsgs.size) maxNumAvailableMsgs = enabledMsgs.size
  }

  def scheduleNextMessage: Option[MessageId] =
    if (priorityMap.keySet.nonEmpty) {
      val maxKey = priorityMap.keySet.max
      val toSchedule = priorityMap(maxKey)
      priorityMap.remove(maxKey)
      Some(schedule(toSchedule))
    } else None


  /**
    * @return true if racy to a concurrently enabled event
    */
  private def isRacy(message: MessageId): Boolean =
    priorityMap.values.exists(e => e != message && InternalProgramEvent.areRacyEvents(messages(e), messages(message)))


  private def schedule(eventId: MessageId): MessageId = {

    val toUpdate: mutable.ListBuffer[Double] = new mutable.ListBuffer[Double]() // sort to determinize the order
      priorityMap.keySet.toList.sorted.foreach(e => if(e != eventId && InternalProgramEvent.areRacyEvents(messages(priorityMap(e)), messages(eventId))) toUpdate.append(e))

    toUpdate.foreach(e => {
      val msgId = priorityMap.remove(e).get
      priorityMap.put(rand.nextDouble(), msgId)
    })

    schedule += eventId
    numScheduled = numScheduled + 1
    eventId
  }

  def isEnabled(id: MessageId): Boolean = preds(id).forall(x => processed.contains(x))

  def getSchedule: List[MessageId] = schedule.toList

  def getMaxNumAvailableMsgs: Int = maxNumAvailableMsgs

  def getNumScheduledMsgs: Int = numScheduled
}

