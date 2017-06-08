package pct

import akka.actor.{Actor, Props}
import protocol._
import akka.dispatch.RequestForwarder

class PCTActor extends Actor {
  private val pctOptions = PCTOptions(maxMessages=10, bugDepth=2)
  private val pctStrategy = new PCTStrategy(pctOptions)
  //The first message should be created otherwise pct crashes!
  val message0 = new Message(0L, Set(), true)
  Messages.putMessage(message0)
  pctStrategy.setNewMessages(List(message0.id))
  
  override def receive: Receive = {
    // The actor receives the created messages and their predecessors at each step of the computation
    // (i.e. after the program initialization and after a message is processed by an actor)
    case MessagePredecessors(predecessors: Map[MessageId, Set[MessageId]]) =>
      println("PCTActor received predecessors: ")
      predecessors.foreach(m => println("Message " + m._1 + " has predecessors -> " + m._2))
      //Update chains with the new messages
      var newMessages: List[MessageId] = List()
      predecessors.foreach {m =>        
        val message = new Message(m._1, m._2) 
        Messages.putMessage(message)
        newMessages = newMessages :+ message.id
      }
      println("newMessagesSize: " + newMessages.size)
      pctStrategy.setNewMessages(newMessages)
      //Select a message using the PCT algorithm
      val nextMessage = pctStrategy.getNextMessage
      println("nextMessage: " + nextMessage.get)
      //Forward dispatch request to the PCTDispatcher:
      nextMessage match {
        case Some(id) => RequestForwarder.forwardRequest(DispatchMessageRequest(id))
        case None => 
      }      
  }
}

object PCTActor {
  val props: Props = Props[PCTActor]
}
