package scheduler.pctcp

import akka.actor.{Actor, Props}
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import akka.dispatch.{DispatcherInterface, DispatcherOptions, ProgramEvent}
import com.typesafe.scalalogging.LazyLogging
import pctcp.TaPCTCPOptions
import protocol.{AddedEvents, DispatchMessageRequest, MessageId, TerminateRequest}
import scheduler.pctcp.ag.TaPCTCPSchedulerAG

class TaPCTCPActor(options: TaPCTCPOptions) extends Actor with LazyLogging {
  private val scheduler = new TaPCTCPSchedulerAG(options)
  CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "TaPCTCP Actor settings: \n" + options.toString)

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    case AddedEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Added messages: " + predecessors.toList.sortBy(_._1))

      //println("Added messages: " + predecessors.toList.sortBy(_._1))
      scheduler.addNewMessages(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]])
      val nextMessage = scheduler.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          //println("Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "TaPCTCP Actor terminating the system")
          //println("TaPCTCP Actor terminating the system")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }
  }

  def logStats(): Unit = {
    val alg = if(options.alg.equals("AG")) "AG" else "BM"
    FileUtils.printToFile("stats") { p =>
      p.println("TaPCTCP scheduler.Scheduler Stats: \n")
      p.println("Algorithm: " + alg)
      p.println("RandomSeed: " + options.randomSeed)
      p.println("MaxRacyMessages: " + options.maxRacyMessages)
      p.println("BugDepth: " + options.bugDepth)
      p.println()
      p.println("NumScheduledMsgs: " + scheduler.getNumScheduledMsgs)
      p.println("MaxNumAvailableChains: " + scheduler.getMaxNumAvailableChains)
      p.println("NumChains: " + scheduler.getNumChains)
      p.println("PrioInversionPoints: " + scheduler.getPrioInvPoints)
      p.println("Schedule: " + scheduler.getSchedule)
      p.println("Chains: " + scheduler.getChainsOfMsgs)
    }
  }
}

object TaPCTCPActor {
  def props(options: TaPCTCPOptions): Props =
    Props(new TaPCTCPActor(options))
  case object LogStats
}
