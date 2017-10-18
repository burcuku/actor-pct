package akka.dispatch.io

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.RequestForwarder
import akka.dispatch.state.Messages.MessageId
import akka.dispatch.util.FileUtils
import protocol._

/**
  * Selects the next message randomly from the set of available messages
  */
class RandomExecProvider(seed: Long = System.currentTimeMillis()) extends IOProvider {
  var randomExecActor: Option[ActorRef] = None

  def setUp(system: ActorSystem): Unit = {
    randomExecActor = Some(system.actorOf(RandomExecActor.props(seed)))
    RequestForwarder.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    randomExecActor match {
      case Some(actor) => actor ! response
      case None => println("The actor for selecting random messages is not created.")
    }
  }
}

// Receives user inputs and displays the received responses
class RandomExecActor(randomSeed: Long) extends Actor {
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
          RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None =>
          logStats()
          println("Quiting. ")
          RequestForwarder.forwardRequest(TerminateRequest)
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
      p.println("Random Scheduler Stats: \n")
      p.println("RandomSeed: " + randomSeed)
      p.println("MaxNumOfConcurrentMsgs: " + maxConcurrentMsgs)
      p.println("NumScheduledMsgs: " + processed.size)
      p.println("Schedule: " + processed.reverse)
    }
  }
}

object RandomExecActor {
  def props(seed: Long): Props = Props(new RandomExecActor(seed)).withDispatcher("akka.actor.pinned-dispatcher")
}
