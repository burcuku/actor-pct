package akka.dispatch.io

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.RequestForwarder
import akka.dispatch.state.Messages.MessageId
import protocol._

/**
  * Selects the next message randomly from the set of available messages
  */
object RandomExecProvider extends IOProvider {
  var randomExecActor: Option[ActorRef] = None

  def setUp(system: ActorSystem): Unit = {
    randomExecActor = Some(system.actorOf(RandomExecActor.props))
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
class RandomExecActor extends Actor {
  val random = scala.util.Random
  private var preds: Map[MessageId, Set[MessageId]] = Map()
  private var messages: Set[MessageId] = Set()
  private var processed: Set[MessageId] = Set(0)

  override def receive: Receive = {

    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      println("Received response: " + predecessors)
      predecessors.keySet.foreach(msg => preds = preds + (msg -> predecessors(msg)))
      messages = messages union predecessors.keySet

      selectRandomMessage match {
        case Some(id) =>
          println("Randomly selected message: " + id)
          processed = processed + id
          RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None =>
          println("Quiting. ")
          RequestForwarder.forwardRequest(TerminateRequest)
      }

    case err: ErrorResponse => println("Error: " + err)
    case _ => println("Undefined message sent to the random executor.")
  }

  def selectRandomMessage: Option[MessageId] = {
    def selectRandom(set: List[MessageId]): MessageId = scala.util.Random.shuffle(set).head

    def isEnabled(id: MessageId): Boolean = preds(id).forall(x => processed.contains(x))

    messages.diff(processed).filter(isEnabled).toList match {
      case x :: xs => Some(selectRandom(x :: xs))
      case Nil => None
    }
  }
}

object RandomExecActor {
  def props: Props = Props[RandomExecActor].withDispatcher("akka.actor.pinned-dispatcher")
}
