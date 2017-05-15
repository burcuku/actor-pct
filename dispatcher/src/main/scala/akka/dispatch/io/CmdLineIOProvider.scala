package akka.dispatch.io

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import CmdLineProcessorActor.GetInput
import akka.dispatch.{ActorMessagesMap, PCTDispatcher, QueryRequestHandler}
import akka.dispatch.util.CmdLineUtils
import protocol._

object CmdLineIOProvider extends IOProvider {
  var cmdLineProcessor: Option[ActorRef] = None

  override def setUp(system: ActorSystem): Unit = {
    cmdLineProcessor = Some(system.actorOf(CmdLineProcessorActor.props))
    cmdLineProcessor.get ! GetInput
  }

  def putResponse(response: QueryResponse): Unit = {
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
      val actorMsgs = PCTDispatcher.listOfActorsByName
      CmdLineUtils.printListOfMap(actorMsgs)

      CmdLineUtils.printlnForUiInput("Please enter the next command: \"start\" OR \"quit\" OR " +
        "  \"next <index>\" to dispatch the next message of an actor OR" +
        "  \"drop <index>\" to drop the next message of an actor")

      val choice = CmdLineUtils.parseInput(Range(0, actorMsgs.size), List("start", "next", "drop", "quit"))

      choice match {
        case ("start", _) =>
          QueryRequestHandler.handleRequest(ActionRequest(QueryRequests.ACTION_INIT, "")) // interpreted as Start
        case ("next", Some(0)) =>
          println("Requested next actor")
          QueryRequestHandler.handleRequest(ActionRequest(QueryRequests.ACTION_NEXT, ""))
        case ("next", Some(actorNo)) =>
          QueryRequestHandler.handleRequest(ActionRequest(QueryRequests.ACTION_NEXT, PCTDispatcher.getActorNameByIndex(actorNo)))
        case ("drop", Some(actorNo)) =>
          QueryRequestHandler.handleRequest(ActionRequest(QueryRequests.ACTION_DROP, PCTDispatcher.getActorNameByIndex(actorNo)))
        case ("quit", _) =>
          println("Requested to quit")
          QueryRequestHandler.handleRequest(ActionRequest(QueryRequests.ACTION_END, ""))
        case _ =>
          CmdLineUtils.printlnForUiInput("Wrong input. Try again.")
          self ! GetInput
      }

    case response: QueryResponse =>
      println("IOProvider received response: " + response)
      self ! GetInput // get next user input once the response is received

    case _ => println("Undefined message sent to the CmdLineProcessorActor")
  }
}

object CmdLineProcessorActor {
  def props: Props = Props(new CmdLineProcessorActor()).withDispatcher("akka.actor.pinned-dispatcher")
  case object GetInput
}
