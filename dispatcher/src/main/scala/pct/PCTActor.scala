package pct

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.RequestForwarder
import akka.dispatch.util.FileUtils
import com.typesafe.scalalogging.LazyLogging
import pct.ag.PCTSchedulerAG
import pct.bm.PCTSchedulerBM

class PCTActor(pctOptions: PCTOptions) extends Actor with LazyLogging {
  private val pctScheduler = if(pctOptions.alg.equals("AG")) new PCTSchedulerAG(pctOptions) else new PCTSchedulerBM(pctOptions)
  logger.warn("\n PCT Actor settings: \n" + pctOptions.toString)

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
          RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None =>
          logger.info("PCT Actor terminating the system")
          println("PCT Actor terminating the system")
          logStats()
          RequestForwarder.forwardRequest(TerminateRequest)
      }
  }

  def logStats(): Unit = {
    val alg = if(pctOptions.alg.equals("AG")) "AG" else "BM"
    FileUtils.printToFile("stats") { p =>
      p.println("PCT Scheduler Stats: \n")
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

object PCTActor {
  def props(pctOptions: PCTOptions): Props =
    Props(new PCTActor(pctOptions))
}
