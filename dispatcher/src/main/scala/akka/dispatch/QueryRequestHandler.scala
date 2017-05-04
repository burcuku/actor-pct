package akka.dispatch

import akka.actor.Cell
import akka.dispatch.util.CmdLineUtils
import protocol._

/**
  * Handles a QueryRequest by calling a particular method of the LoggingDispatcher
  * The called methods of the LoggingDispatcher posts the required job on the dispatcher thread
  *  (executed async on the dispatcher thread, not on the caller thread)
  */
object QueryRequestHandler {
  def handleRequest(request: QueryRequest): Unit = request match {

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_INIT) => // Start Request
      CmdLineUtils.printlnForLogging("===== Sending initial list of events..")
      PCTDispatcher.initiateDispatcher()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_END) => // Terminate Request
      CmdLineUtils.printlnForLogging("===== Requested to terminate: ")
      PCTDispatcher.terminateDispatcher()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_DROP) => // Next Actor in the Replayed Trace
      CmdLineUtils.printlnForLogging("===== Requested next actor: ")
      PCTDispatcher.dropActorMsg(receiverId)

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_NEXT) && receiverId.equals("")=> // Next Actor in the Replayed Trace
      CmdLineUtils.printlnForLogging("===== Requested next actor: ")
      PCTDispatcher.dispatchToNextActor()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_NEXT) =>
      CmdLineUtils.printlnForLogging("===== Selected next actor: " + receiverId)
      PCTDispatcher.dispatchToActor(receiverId)

    case _ => System.err.println("Unidentified request")
  }
}

