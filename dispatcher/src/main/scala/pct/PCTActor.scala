package pct

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.RequestForwarder
import akka.dispatch.util.DispatcherUtils
import com.typesafe.config.ConfigFactory

class PCTActor extends Actor {
  private val pctOptions = PCTOptions(maxMessages = ConfigFactory.load(DispatcherUtils.dispatcherConfigFile).getInt("pct-dispatcher.maxMessages"),
                                      bugDepth = ConfigFactory.load(DispatcherUtils.dispatcherConfigFile).getInt("pct-dispatcher.bugDepth"))
  private val pctStrategy = new PCTStrategy(pctOptions)
  
  //The first message should be created otherwise pct crashes!
  pctStrategy.setNewMessages(Map(0L->Set()))
  pctStrategy.getNextMessage
    
  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    // (i.e. after the program initialization and after a message is processed by an actor)
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      //println("PCTActor received predecessors: ")
      //predecessors.foreach(m => println("Message " + m._1 + " has predecessors -> " + m._2))
      //Update chains with the new messages
      pctStrategy.setNewMessages(predecessors)
      //Select a message using the PCT algorithm
      val nextMessage = pctStrategy.getNextMessage
      //Forward dispatch request to the PCTDispatcher:
      nextMessage match {
        case Some(id) => RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None => RequestForwarder.forwardRequest(TerminateRequest) 
      }      
  }
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
