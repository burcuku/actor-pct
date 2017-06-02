package pct

import akka.actor.{Actor, Props}
import protocol._

class PCTActor extends Actor {

  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    // (i.e. after the program initialization and after a message is processed by an actor)
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      println("PCTActor received predecessors: ")
      predecessors.foreach(m => println("Message " + m._1 + " has predecessors -> " + m._2))

      // todo Update chains with the new messages
      // Select a message using the PCT algorithm
      // Forward dispatch request to the PCTDispatcher:
      // RequestForwarder.forwardRequest(DispatchMessageRequest(selectedMessageId))
  }
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
