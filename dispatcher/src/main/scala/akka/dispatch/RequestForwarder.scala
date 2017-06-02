package akka.dispatch

import akka.dispatch.util.CmdLineUtils
import protocol.{InitRequest, _}

/**
  * Forwards a dispatching request to the PCTDispatcher
  */
object RequestForwarder {
  def forwardRequest(request: Request): Unit = request match {

    case InitRequest => // Start Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Sending initial list of events..")
      PCTDispatcher.initiateDispatcher()

    case DispatchMessageRequest(messageId) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Selected next message: " + messageId)
      PCTDispatcher.dispatchMessage(messageId)

    case DropMessageRequest(messageId) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Selected to drop next message: " + messageId)
      PCTDispatcher.dropMessage(messageId)

    case TerminateRequest => // Terminate Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Requested to terminate: ")
      PCTDispatcher.terminateDispatcher()

    case _ => System.err.println("Unidentified request")
  }
}

