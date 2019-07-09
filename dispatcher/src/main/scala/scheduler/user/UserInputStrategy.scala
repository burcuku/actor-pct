package scheduler.user

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.TestingDispatcher.AddedEvents
import akka.dispatch.{DispatcherInterface, InternalProgramEvent, SchedulingStrategy}
import akka.dispatch.util.CmdLineUtils
import explorer.protocol.{DispatchMessageRequest, InitRequest, MessageId, Response, TerminateRequest}
import scheduler.user.UserInputActor.GetInput

object UserInputStrategy extends SchedulingStrategy {
  var cmdLineProcessor: Option[ActorRef] = None

  override def setUp(system: ActorSystem): Unit = {
    cmdLineProcessor = Some(system.actorOf(UserInputActor.props))
    DispatcherInterface.forwardRequest(InitRequest)
  }

  def putResponse(response: Response): Unit = {
    cmdLineProcessor match {
      case Some(actor) => actor ! response
      case None => println("The actor for processing command line IO is not created.")
    }
  }
}

// Receives user inputs and displays the received responses
class UserInputActor extends Actor {

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
          DispatcherInterface.forwardRequest(DispatchMessageRequest(messageId))
        case ("quit", _) =>
          DispatcherInterface.forwardRequest(TerminateRequest)
        case _ =>
          CmdLineUtils.printlnForUiInput("Wrong input. Try again.")
          self ! GetInput
      }

    case AddedEvents(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      println("Predecessors: ") // MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]])
      predecessors.foreach(x => println(x._1 + " -> " + x._2))
      self ! GetInput // get next user input once the response is received

    case _ =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "Undefined message sent to the CmdLineProcessorActor")
  }
}

object UserInputActor {
  def props: Props = Props(new UserInputActor()).withDispatcher("akka.actor.pinned-dispatcher")
  case object GetInput
}
