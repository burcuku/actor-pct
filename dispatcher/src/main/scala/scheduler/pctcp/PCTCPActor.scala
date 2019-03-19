package scheduler.pctcp

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.DispatcherInterface
import akka.dispatch.util.FileUtils
import com.typesafe.scalalogging.LazyLogging
import pctcp.PCTCPOptions
import scheduler.Scheduler.MessageId
import scheduler.pctcp.ag.PCTCPSchedulerAG
import scheduler.pctcp.bm.PCTCPSchedulerBM

class PCTCPActor(pctOptions: PCTCPOptions) extends Actor with LazyLogging {
  private val pctScheduler = if(pctOptions.alg.equals("AG")) new PCTCPSchedulerAG(pctOptions) else new PCTCPSchedulerBM(pctOptions)
  logger.warn("\n PCTCP Actor settings: \n" + pctOptions.toString)

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      logger.debug("Added messages: " + predecessors.toList.sortBy(_._1))

      pctScheduler.addNewMessages(predecessors)
      val nextMessage = pctScheduler.scheduleNextMessage

      nextMessage match {
        case Some(id) =>
          logger.info("Selected message: " + id)
          println("Selected message: " + id)
          DispatcherInterface.forwardRequest(DispatchMessageRequest(id))
        case None =>
          logger.info("PCTCP Actor terminating the system")
          println("PCTCP Actor terminating the system")
          logStats()
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
