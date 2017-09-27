package pct

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.RequestForwarder
import pct.ag.PCTSchedulerAG
import pct.bm.PCTSchedulerBM

class PCTActor extends Actor {
  private val pctOptions = PCTOptions(Options.maxMessages, Options.bugDepth)
  private val pctScheduler = if(Options.algorithm.equals("AG")) new PCTSchedulerAG(pctOptions) else new PCTSchedulerBM(pctOptions)
  Options.print()

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      pctScheduler.addNewMessages(predecessors)
      val nextMessage = pctScheduler.getNextMessage
      nextMessage match {
        case Some(id) =>
          RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None =>
          println(pctScheduler.getSchedule)
          RequestForwarder.forwardRequest(TerminateRequest)
      }
  }
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
