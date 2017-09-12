package akka.dispatch.io

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import CmdLineProcessorActor.GetInput
import akka.dispatch.state.Messages.MessageId
import akka.dispatch.RequestForwarder
import akka.dispatch.util.CmdLineUtils
import protocol._

object CmdLineIOProvider extends IOProvider {
  var cmdLineProcessor: Option[ActorRef] = None

  override def setUp(system: ActorSystem): Unit = {
    cmdLineProcessor = Some(system.actorOf(CmdLineProcessorActor.props))
    RequestForwarder.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    cmdLineProcessor match {
      case Some(actor) => actor ! response
      case None => println("The actor for processing command line IO is not created.")
    }
  }
}

// Receives user inputs and displays the received responses
class CmdLineProcessorActor extends Actor {

  val processed: Set[MessageId] = Set()

  override def receive: Receive = {
    // blocking wait for an input
    case GetInput =>

      CmdLineUtils.printlnForUiInput("Please enter the next command: " +
        " \"next <index>\" to dispatch the message with id <index> OR " +
        //" \"drop <index>\" to drop the the message with id <index> OR " +
        "\"quit\" to quit. ")

      val choice = CmdLineUtils.parseInput(List("next", "drop", "quit"))

      choice match {
        case ("next", messageId) =>
          RequestForwarder.forwardRequest(DispatchMessageRequest(messageId))
        case ("drop", messageId) =>
          RequestForwarder.forwardRequest(DropMessageRequest(messageId))
        case ("quit", _) =>
          RequestForwarder.forwardRequest(TerminateRequest)
        case _ =>
          CmdLineUtils.printlnForUiInput("Wrong input. Try again.")
          self ! GetInput
      }

    case MessagePredecessors(predecessors) =>
      println("Predecessors: ") // MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]])
      predecessors.foreach(x => println(x._1 + " -> " + x._2))
      self ! GetInput // get next user input once the response is received

    case ErrorResponse(errorMsg) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, errorMsg)
      self ! GetInput // get next user input once the response is received

    case _ =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "Undefined message sent to the CmdLineProcessorActor")
  }
}

object CmdLineProcessorActor {
  def props: Props = Props(new CmdLineProcessorActor()).withDispatcher("akka.actor.pinned-dispatcher")
  case object GetInput
}
