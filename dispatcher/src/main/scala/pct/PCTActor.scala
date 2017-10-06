package pct

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.RequestForwarder
import com.typesafe.scalalogging.LazyLogging
import pct.ag.PCTSchedulerAG
import pct.bm.PCTSchedulerBM

class PCTActor extends Actor with LazyLogging {
  private val pctOptions = PCTOptions(randomSeed = System.currentTimeMillis(), maxMessages = Options.maxMessages, bugDepth = Options.bugDepth)
  private val pctScheduler = if(Options.algorithm.equals("AG")) new PCTSchedulerAG(pctOptions) else new PCTSchedulerBM(pctOptions)
  logger.warn(pctOptions.toString)

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      logger.debug("Added messages: " + predecessors.toList.sortBy(_._1))

      pctScheduler.addNewMessages(predecessors)
      val nextMessage = pctScheduler.getNextMessage

      nextMessage match {
        case Some(id) =>
          logger.info("Selected message: " + id)
          RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None =>
          logger.info("Terminating")
          logger.info("Schedule: \n" + pctScheduler.getSchedule)
          RequestForwarder.forwardRequest(TerminateRequest)
      }
  }
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
