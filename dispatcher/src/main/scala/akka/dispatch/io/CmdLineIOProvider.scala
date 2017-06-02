package akka.dispatch.io

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import CmdLineProcessorActor.GetInput
import akka.dispatch.{ActorMessagesMap, PCTDispatcher, RequestHandler}
import akka.dispatch.util.CmdLineUtils
import protocol._

object CmdLineIOProvider extends IOProvider {
  var cmdLineProcessor: Option[ActorRef] = None

  override def setUp(system: ActorSystem): Unit = {
    cmdLineProcessor = Some(system.actorOf(CmdLineProcessorActor.props))
    cmdLineProcessor.get ! GetInput
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

  override def receive: Receive = {
    // blocking wait for an input
    case GetInput =>
      //val actorMsgs = PCTDispatcher.listOfActorsByName
      //CmdLineUtils.printListOfMap(actorMsgs)
      PCTDispatcher.printAllActorMessages

      CmdLineUtils.printlnForUiInput("Please enter the next command: \"start\" OR \"quit\" OR " +
        " \"next <index>\" to dispatch the message with id <index> OR" +
        " \"drop <index>\" to drop the the message with id")

      val choice = CmdLineUtils.parseInput(1 to 10, List("start", "next", "drop", "quit"))

      choice match {
        case ("start", _) =>
          RequestHandler.handleRequest(InitRequest) // interpreted as Start
        case ("next", Some(0)) =>
          println("Requested next actor")
          RequestHandler.handleRequest(DispatchToActorRequest(""))
        case ("next", Some(messageId)) =>
          RequestHandler.handleRequest(DispatchMessageRequest(messageId))
        case ("drop", Some(messageId)) =>
          RequestHandler.handleRequest(DropMessageRequest(messageId))
        case ("quit", _) =>
          println("Requested to quit")
          RequestHandler.handleRequest(TerminateRequest)
        case _ =>
          CmdLineUtils.printlnForUiInput("Wrong input. Try again.")
          self ! GetInput
      }

    case response: Response =>
      //println("IOProvider received response: " + response)
      self ! GetInput // get next user input once the response is received

    case _ => println("Undefined message sent to the CmdLineProcessorActor")
  }
}

object CmdLineProcessorActor {
  def props: Props = Props(new CmdLineProcessorActor()).withDispatcher("akka.actor.pinned-dispatcher")
  case object GetInput
}
