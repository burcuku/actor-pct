package server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.{DispatcherInterface, DispatcherOptions, ProgramEvent}
import akka.dispatch.util.CmdLineUtils
import controller.{StateManager, StateManagerdfs}
import protocol.{AddedEvents, DispatchMessageRequest, MessageId, TerminateRequest}

class ExplorerActor(server: ActorRef) extends Actor with ActorLogging {


  def logStats():Unit = {println("Please define logStats")}

  private val stateManager = new StateManagerdfs()



  override def receive: Receive = {
    // will receive Configuration(str: String) and send CommandRequest to the server


    // jatin: imo it should receive a set of events
    case AddedEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Added messages: " + predecessors.toList.sortBy(_._1))

      stateManager.addNewMessages(events, predecessors)
      val nextMessage = stateManager.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "One branch done")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }


    case _ =>  // server ! Initiate
  }


}

object ExplorerActor {
  def props(server: ActorRef) = Props(new ExplorerActor(server))
}