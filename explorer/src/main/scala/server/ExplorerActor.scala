package server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import controller.StateManagerdfs
import explorer.protocol.{NewEvents, DispatchMessageRequest, MessageId, ProgramEvent}

class ExplorerActor(server: ActorRef) extends Actor with ActorLogging {


  def logStats():Unit = {println("Please define logStats")}

  private val stateManager = new StateManagerdfs()



  override def receive: Receive = {
    // jatin: imo it should receive a set of events
    case NewEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      println("Explorer Received: " + events)
      server ! DispatchMessageRequest(1)

    /*stateManager.addNewMessages(events, predecessors)
      val nextMessage = stateManager.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "One branch done")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }*/

    case _ =>
  }
}

object ExplorerActor {
  def props(server: ActorRef) = Props(new ExplorerActor(server))
}