package scheduler.pctcp

import akka.actor.{Actor, Props}
import protocol.{AddedEvents, DispatchMessageRequest, MessageId, TerminateRequest}
import akka.dispatch.{DispatcherInterface, DispatcherOptions, ProgramEvent}
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import com.typesafe.scalalogging.LazyLogging
import pctcp.PCTCPOptions
import scheduler.pctcp.ag.PCTCPSchedulerAG
import scheduler.pctcp.bm.PCTCPSchedulerBM

class PCTCPActor(pctOptions: PCTCPOptions) extends Actor with LazyLogging {
  private val pctScheduler = if(pctOptions.alg.equals("AG")) new PCTCPSchedulerAG(pctOptions) else new PCTCPSchedulerBM(pctOptions)
  CmdLineUtils.printLog(CmdLineUtils.LOG_WARNING, "PCTCP Actor settings: \n" + pctOptions.toString)

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    case AddedEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Added messages: " + predecessors.toList.sortBy(_._1))

      pctScheduler.addNewMessages(events, predecessors)
      val nextMessage = pctScheduler.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Selected message: " + id)
          //println("Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "PCTCP Actor terminating the system")
          //println("PCTCP Actor terminating the system")
          if(DispatcherOptions.logStats) logStats()
          DispatcherInterface.forwardRequest(TerminateRequest)
      }
  }

  def logStats(): Unit = {
    val alg = if(pctOptions.alg.equals("AG")) "AG" else "BM"
    FileUtils.printToFile("stats") { p =>
      p.println("PCTCP scheduler.Scheduler Stats: \n")
      p.println("Algorithm: " + alg)
      p.println("RandomSeed: " + pctOptions.randomSeed)
      p.println("MaxMessages: " + pctOptions.maxMessages)
      p.println("BugDepth: " + pctOptions.bugDepth)
      p.println()
      p.println("NumScheduledMsgs: " + pctScheduler.getNumScheduledMsgs)
      p.println("MaxNumAvailableChains: " + pctScheduler.getMaxNumAvailableChains)
      p.println("NumChains: " + pctScheduler.getNumChains)
      p.println("PrioInversionPoints: " + pctScheduler.getPrioInvPoints)
      p.println("Schedule: " + pctScheduler.getSchedule)
      p.println("Chains: " + pctScheduler.getChainsOfMsgs)
    }
  }
}

object PCTCPActor {
  def props(pctcpOptions: PCTCPOptions): Props =
    Props(new PCTCPActor(pctcpOptions))
  case object LogStats
}
