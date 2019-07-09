package scheduler.pos

import akka.actor.{Actor, Props}
import akka.dispatch.TestingDispatcher.AddedEvents
import akka.dispatch.{DispatcherInterface, DispatcherOptions, InternalProgramEvent}
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import com.typesafe.scalalogging.LazyLogging
import explorer.protocol.{DispatchMessageRequest, MessageId, TerminateRequest}

class POSActor(posOptions: POSOptions) extends Actor with LazyLogging {
  private val posScheduler = new POSScheduler(posOptions)
  CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "POS Actor settings: \n" + posOptions.toString)

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
      p.println("Random scheduler.Scheduler Stats: \n")
      p.println("RandomSeed: " + posOptions.randomSeed)
      p.println("MaxNumOfConcurrentMsgs: " + posScheduler.getMaxNumAvailableMsgs)
      p.println("NumScheduledMsgs: " + posScheduler.getNumScheduledMsgs)
      p.println("Schedule: " + posScheduler.getSchedule)
    }
  }
}

object POSActor {
  def props(options: POSOptions): Props = Props(new POSActor(options)).withDispatcher("akka.actor.pinned-dispatcher")
}