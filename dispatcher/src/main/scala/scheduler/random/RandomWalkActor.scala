package scheduler.random

import akka.actor.{Actor, Props}
import akka.dispatch.DispatcherInterface
import akka.dispatch.state.Messages.MessageId
import akka.dispatch.util.FileUtils
import pct.RandomWalkOptions
import protocol.{DispatchMessageRequest, ErrorResponse, MessagePredecessors, TerminateRequest}

// Receives user inputs and displays the received responses
class RandomWalkActor(randomSeed: Long) extends Actor {
  private val random = scala.util.Random
  random.setSeed(randomSeed)
  private var preds: Map[MessageId, Set[MessageId]] = Map()
  private var messages: Set[MessageId] = Set()
  private var processed: List[MessageId] = List(0)
  private var maxConcurrentMsgs = 0 // for stats

  override def receive: Receive = {

    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      println("Received response: " + predecessors)
      predecessors.keySet.foreach(msg => preds = preds + (msg -> predecessors(msg)))
      messages = messages union predecessors.keySet

      selectRandomMessage match {
        case Some(id) =>
          println("Randomly selected message: " + id)
          processed = id :: processed
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          logStats()
          println("Quiting. ")
          DispatcherInterface.forwardRequest(TerminateRequest)
      }

    case err: ErrorResponse => println("Error: " + err)
    case _ => println("Undefined message sent to the random executor.")
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