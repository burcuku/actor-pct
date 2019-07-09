package scheduler.random

import akka.actor.{Actor, Props}
import akka.dispatch.TestingDispatcher.AddedEvents
import akka.dispatch.{DispatcherInterface, DispatcherOptions, InternalProgramEvent}
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import explorer.protocol.{DispatchMessageRequest, MessageId, TerminateRequest}

// Receives user inputs and displays the received responses
class RandomWalkActor(randomSeed: Long) extends Actor {
  private val random = scala.util.Random
  random.setSeed(randomSeed)
  private var preds: Map[MessageId, Set[MessageId]] = Map()
  private var messages: Set[MessageId] = Set()
  private var processed: List[MessageId] = List(0)
  private var maxConcurrentMsgs = 0 // for stats

  override def receive: Receive = {

    case AddedEvents(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Received response: " + predecessors)
      predecessors.keySet.foreach(msg => preds = preds + (msg -> predecessors(msg)))
      messages = messages union predecessors.keySet

      selectRandomMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Randomly selected message: " + id)
          processed = id :: processed
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          if(DispatcherOptions.logStats) logStats()
          //CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Quiting. ")
          DispatcherInterface.forwardRequest(TerminateRequest)
      }
    case _ => CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "Undefined message sent to the random executor.")
  }

  def selectRandomMessage: Option[MessageId] = {
    def selectRandom(set: List[MessageId]): MessageId = random.shuffle(set).head

    def isEnabled(id: MessageId): Boolean = preds(id).forall(x => processed.contains(x))

    val enabledMsgs = messages.diff(processed.toSet).filter(isEnabled).toList

    if(maxConcurrentMsgs < enabledMsgs.size) maxConcurrentMsgs = enabledMsgs.size

    enabledMsgs match {
      case x :: xs => Some(selectRandom(x :: xs))
      case Nil => None
    }
  }

  def logStats(): Unit = {
    FileUtils.printToFile("stats") { p =>
      p.println("Random scheduler.Scheduler Stats: \n")
      p.println("RandomSeed: " + randomSeed)
      p.println("MaxNumOfConcurrentMsgs: " + maxConcurrentMsgs)
      p.println("NumScheduledMsgs: " + processed.size)
      p.println("Schedule: " + processed.reverse)
    }
  }
}

object RandomWalkActor {
  def props(options: RandomWalkOptions): Props = Props(new RandomWalkActor(options.randomSeed)).withDispatcher("akka.actor.pinned-dispatcher")
}