package scheduler.rapos

import akka.actor.{Actor, Props}
import akka.dispatch.TestingDispatcher.AddedEvents
import akka.dispatch.{DispatcherInterface, DispatcherOptions, InternalProgramEvent}
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.{DispatchMessageRequest, MessageId, TerminateRequest}

class RaposActor(raposOptions: RaposOptions) extends Actor with LazyLogging {
  private val posScheduler = new RaposScheduler(raposOptions)
  CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "Rapos Actor settings: \n" + raposOptions.toString)

  override def receive: Receive = {

    case AddedEvents(events: List[(MessageId, InternalProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      //logger.debug("Added messages: " + predecessors.toList.sortBy(_._1))

      posScheduler.addNewMessages(events, predecessors)
      val nextMessage = posScheduler.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "POS Actor terminating the system")
          //println("POS Actor terminating the system")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }
  }

  def logStats(): Unit = {
    FileUtils.printToFile("stats") { p =>
      p.println("Rapos scheduler.Scheduler Stats: \n")
      p.println("RandomSeed: " + raposOptions.randomSeed)
      p.println("MaxNumOfConcurrentMsgs: " + posScheduler.getMaxNumAvailableMsgs)
      p.println("NumScheduledMsgs: " + posScheduler.getNumScheduledMsgs)
      p.println("Schedule: " + posScheduler.getSchedule)
    }
  }
}

object RaposActor {
  def props(options: RaposOptions): Props = Props(new RaposActor(options)).withDispatcher("akka.actor.pinned-dispatcher")
}
