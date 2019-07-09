package akka.dispatch

import akka.dispatch.util.CmdLineUtils
import explorer.protocol.{DispatchMessageRequest, InitRequest, Request, TerminateRequest}

/**
  * Forwards a dispatching request to the Dispatcher
  */
object DispatcherInterface {
  def forwardRequest(request: Request): Unit = request match {

    case InitRequest => // Start Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Sending initial list of events..")
      TestingDispatcher.initiateDispatcher()

    case DispatchMessageRequest(messageId) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Selected next message: " + messageId)
      TestingDispatcher.dispatchMessage(messageId)

    case TerminateRequest => // Terminate Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Requested to terminate: ")
      TestingDispatcher.terminateDispatcher()

    case _ => System.err.println("Unidentified request")
  }
}

