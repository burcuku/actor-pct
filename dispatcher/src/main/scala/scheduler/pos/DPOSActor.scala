package scheduler.pos

import akka.actor.{Actor, Props}
import akka.dispatch.{DispatcherInterface, DispatcherOptions, ProgramEvent}
import akka.dispatch.state.Messages.MessageId
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import com.typesafe.scalalogging.LazyLogging
import protocol.{AddedEvents, DispatchMessageRequest, TerminateRequest}

class DPOSActor(dposOptions: DPOSOptions) extends Actor with LazyLogging {
  private val dposScheduler = new DPOSScheduler(dposOptions)
  CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "DPOS Actor settings: \n" + dposOptions.toString)

  override def receive: Receive = {

    case AddedEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      //logger.debug("Added messages: " + predecessors.toList.sortBy(_._1))

      dposScheduler.addNewMessages(events, predecessors)
      val nextMessage = dposScheduler.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          //println("Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "DPOS Actor terminating the system")
          //println("DPOS Actor terminating the system")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }
  }

  def logStats(): Unit = {
    FileUtils.printToFile("stats") { p =>
      p.println("Random scheduler.Scheduler Stats: \n")
      p.println("RandomSeed: " + dposOptions.randomSeed)
      p.println("BugDepth: " + dposOptions.bugDepth)
      p.println("MaxNumOfConcurrentMsgs: " + dposScheduler.getMaxNumAvailableMsgs)
      p.println("NumScheduledMsgs: " + dposScheduler.getNumScheduledMsgs)
      p.println("Schedule: " + dposScheduler.getSchedule)
    }
  }
}

object DPOSActor {
  def props(options: DPOSOptions): Props = Props(new DPOSActor(options)).withDispatcher("akka.actor.pinned-dispatcher")
}