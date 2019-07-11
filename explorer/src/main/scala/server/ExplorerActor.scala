package server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import controller.StateManagerdfs
import explorer.protocol.{DispatchMessageRequest, MessageId, NewEvents, ProgramEvent, TerminateRequest}

class ExplorerActor(server: ActorRef) extends Actor with ActorLogging {


  def logStats():Unit = {println("Please define logStats")}

  private val stateManager = new StateManagerdfs()



  override def receive: Receive = {
    // jatin: imo it should receive a set of events
    case NewEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      println("Explorer Received: " + events)
      println("Predecessors: " + predecessors)

      //    server ! DispatchMessageRequest(1)

      stateManager.addNewMessages(events, predecessors)
      val nextMessage = stateManager.scheduleNextMessage

      System.exit(0)


      nextMessage match {
        case Some(id) =>
          println("Selected message: " + id)
          server ! DispatchMessageRequest(id)
        case None =>
          println("One branch done")
          server! TerminateRequest
      }

    case _ =>
  }
}

object ExplorerActor {
  def props(server: ActorRef) = Props(new ExplorerActor(server))
}