package akka.dispatch

import explorer.protocol.{DispatchMessageRequest, InitRequest, Request, TerminateRequest}

/**
  * Handles a Request by calling a particular method of the Dispatcher
  * The called methods of the Dispatcher posts the required job on the dispatcher thread
  *  (executed async on the dispatcher thread, not on the caller thread)
  */
object RequestHandler {
  def handleRequest(request: Request): Unit = request match {
    //todo more
    case DispatchMessageRequest(id) => // Start Request
      //CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Sending initial list of events..")
      //DebuggingDispatcher.initiateDispatcher()
      println("Query request handler is here!")
      TestingDispatcher.dispatchMessage(id)

    case InitRequest => // Start Request
      println("Query request handler is here! Initiating!")
      TestingDispatcher.initiateDispatcher()

    case TerminateRequest => // Start Request
      println("Query request handler is here! Terminating!")
      TestingDispatcher.terminateDispatcher()

    case _ => System.err.println("Unidentified request")
  }
}
