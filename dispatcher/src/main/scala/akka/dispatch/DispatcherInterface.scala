package akka.dispatch

import akka.dispatch.util.CmdLineUtils
import protocol.{InitRequest, _}

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

    case DropMessageRequest(messageId) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Selected to drop next message: " + messageId)
      TestingDispatcher.dropMessage(messageId)

    case TerminateRequest => // Terminate Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "===== Requested to terminate: ")
      TestingDispatcher.terminateDispatcher()

    case _ => System.err.println("Unidentified request")
  }
}

