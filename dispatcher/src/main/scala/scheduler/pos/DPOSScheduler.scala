package scheduler.pos

import akka.dispatch.{MessageSent, ProgramEvent}
import com.typesafe.scalalogging.LazyLogging
import protocol.MessageId
import scheduler.Scheduler

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class DPOSScheduler(options: DPOSOptions) extends Scheduler with LazyLogging {
  private val rand = new Random(options.randomSeed)

  private var priorityMap: mutable.Map[Double, MessageId] = new mutable.HashMap[Double, MessageId]()
  // assigns ith racy event to a priority < 0
  private val prioritiesToBeUpdated: Map[Int, Double] = getRandomChangePoints(options.bugDepth - 1)
  private val priorityChangedAt: ListBuffer[MessageId] = new ListBuffer()

  private var maxNumAvailableMsgs = 0 // for stats
  // for checking the number of concurrently enabled messages
  private var messages: mutable.Map[MessageId, ProgramEvent] = new mutable.HashMap[MessageId, ProgramEvent]()
  private var processed: List[MessageId] = List(0)
  private var preds: Map[MessageId, Set[MessageId]] = Map()

  private var numScheduled: Int = 0
  private var schedule: ListBuffer[MessageId] = ListBuffer(0)

  private var currentNumRacyEvents = 0
  private var racyEvents: ListBuffer[MessageId] = ListBuffer(0)

  def addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]): Unit = {
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
      Some(schedule(maxKey, toSchedule))
    } else None


  /**
    * @return true if racy to a concurrently enabled event
    */
  private def isRacy(message: MessageId): Boolean =
    priorityMap.values.exists(e => ProgramEvent.areRacyEvents(messages(e), messages(message)))


  private def schedule(maxKey: Double, eventId: MessageId): MessageId = {
    if(!racyEvents.contains(eventId) && isRacy(eventId)) {
      racyEvents.append(eventId)
      currentNumRacyEvents += 1
    }

    if(prioritiesToBeUpdated.keySet.contains(currentNumRacyEvents) && !priorityChangedAt.contains(eventId)) {
      logger.debug("Changing priority of event: " + eventId + " to " + prioritiesToBeUpdated(currentNumRacyEvents))
      priorityMap.remove(maxKey)
      priorityMap.put(rand.nextDouble(), eventId)
      priorityChangedAt.append(eventId)

      val k = priorityMap.keySet.max
      return schedule(k, priorityMap(k))
    }

    priorityMap.remove(maxKey)
    schedule += eventId
    numScheduled = numScheduled + 1
    eventId
  }

  private def getRandomChangePoints(numChPoints: Int): Map[Int, Double] = {
    var counter = 1
    var allPossibleChPoints = new ListBuffer[Int]()

    (0 until options.maxRacyMessages).foreach( i => {
      allPossibleChPoints.append(counter)
      counter += 1
    })

    def getRandomFromList: Int = {
      val v = rand.nextInt(allPossibleChPoints.length)
      allPossibleChPoints.remove(v)
      v
    }

    val map: mutable.Map[Int, Double] = new mutable.HashMap[Int, Double]()
    (0 until numChPoints).foreach(i => map.put(getRandomFromList, rand.nextDouble() - 1))
    map.toMap
  }

  def isEnabled(id: MessageId): Boolean = preds(id).forall(x => processed.contains(x))

  def getSchedule: List[MessageId] = schedule.toList

  def getMaxNumAvailableMsgs: Int = maxNumAvailableMsgs

  def getNumScheduledMsgs: Int = numScheduled
}

