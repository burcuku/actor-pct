package akka.dispatch

import akka.actor.{Actor, ActorRef, Props}
import akka.dispatch.PCTDispatcher.{Message, MessageId}

/**
  * Maintains the dependencies between the messages of a program
  */
class DependencyGraphBuilder {
  /**
    *  Causality constraints (the key is causally dependent on the set of the values):
    */
  private var messageSendCausalityMap: Map[MessageId, Set[MessageId]] = Map()
  private var creatorCausalityMap: Map[MessageId, Set[MessageId]] = Map()
  private var executedOnSenderCausalityMap: Map[MessageId, Set[MessageId]] = Map()
  private var executedOnCreatorCausalityMap: Map[MessageId, Set[MessageId]] = Map()

  /**
    *  Sender-receiver constraints
    */
  private var senderReceiverCausalityMap: Map[MessageId, Set[MessageId]] = Map()

  /**
    *  Causality predecessors of a message
    */
  def causalityPredecessors(id: MessageId): Set[MessageId] = (messageSendCausalityMap.getOrElse(id, Set()) ++ creatorCausalityMap.getOrElse(id, Set())
    ++ executedOnSenderCausalityMap.getOrElse(id, Set()) ++ executedOnCreatorCausalityMap.getOrElse(id, Set()))

  /**
    *  Predecessors of a message: union of all causality + sender-receiver constraints
    */
  def predecessors(id: MessageId): Set[MessageId] = causalityPredecessors(id) ++ senderReceiverCausalityMap.getOrElse(id, Set())

  /**
    *  Helper structures
    */
  private var actorProcessedMap: Map[ActorRef, Set[Message]] = Map()
  private var actorCreatedByMap: Map[ActorRef, ActorRef] = Map()
  private var actorCreatedInMap: Map[ActorRef, Message] = Map()
  private var messagesSentByMap: Map[ActorRef, List[Message]] = Map()

  private val hbRelations: Set[(Message, Message) => Unit] =
    Set(addMessageSendCausality, addCreatorHBCausality, addExecutedOnSenderCausality, addExecutedOnCreatorCausality, addSenderReceiverCausality)

  /**
    * If a message mj is sent in a message mi, mi -> mj
    * @param cause The message received and processed
    * @param dependent The message created when cause is processed
    */
  private def addMessageSendCausality(cause: Message, dependent: Message): Unit = messageSendCausalityMap += (dependent.id -> Set(cause.id))

  /**
    * If the receiver of a message mj is created in a message mi, mi -> mj
    * @param cause The message received and processed
    * @param dependent The message created when cause is processed
    */
  private def addCreatorHBCausality(cause: Message, dependent: Message): Unit = actorCreatedInMap.get(dependent.receiver) match {
    case Some(messageThatCreatedReceiver) => creatorCausalityMap += (dependent.id -> Set(messageThatCreatedReceiver.id))
    case None => // do nth
  }

  /**
    * If a message mj is sent in a message mk on an actor A, for all the messages i<k<j processed by A before k: mi -> mj
    * @param cause The message received and processed
    * @param dependent The message created when cause is processed
    */
  private def addExecutedOnSenderCausality(cause: Message, dependent: Message): Unit =
    executedOnSenderCausalityMap += (dependent.id -> actorProcessedMap.getOrElse(cause.receiver, Set()).map(_.id))

  /**
    * If the receiver of a message mj is created in a message mk on an actor A, for all the messages i<k<j processed by A before k: mi -> mj
    * @param cause The message received and processed
    * @param dependent The message created when cause is processed
    */
  private def addExecutedOnCreatorCausality(cause: Message, dependent: Message): Unit = actorCreatedByMap.get(dependent.receiver) match {
    case Some(actorThatCreatedReceiver) =>
      executedOnCreatorCausalityMap += (dependent.id -> actorProcessedMap.getOrElse(actorThatCreatedReceiver, Set()).map(_.id))
    case None => // do nth
  }

  /**
    * If two messages mi and mj have same sender and receiver where mi is sent before, mi -> mj
    * Add the last message from the same sender to the same receiver to the causality dependency
    * @param cause The message received and processed
    * @param dependent The message created when cause is processed
    */
  private def addSenderReceiverCausality(cause: Message, dependent: Message): Unit = {

    def lastMessageFrom(sender: ActorRef, receiver: ActorRef): Option[Message] =
      messagesSentByMap.getOrElse(sender, List()).find(m => m.receiver == receiver && m.msg.sender == sender)

    lastMessageFrom(cause.receiver, dependent.receiver) match {
      case Some(message) => senderReceiverCausalityMap += (dependent.id -> Set(message.id))
      case None => // do nth
    }
  }

  /**
    * Updates the dependency graph with the received message "received" and the events generated during processing it
    * Used by the PCT dispatcher to maintain the dependencies between the messages in the program
    * @param received the message received/processed
    * @param sent the list of messages created and sent during the processing of "received"
    * @param created the list of actors created during the processing of "received"
    * @return predecessors of thesent messages
    */
  def calculateDependencies (received: Message, sent: List[Message], created: List[ActorRef]): Map[MessageId, Set[MessageId]] = {
    // update creation helper structures before forming hb relations
    created.foreach(newActor => {
      actorCreatedInMap += newActor -> received
      actorCreatedByMap += newActor -> received.receiver
    })

    sent.foreach(message => {
      hbRelations.foreach(f => f(received, message))
      messagesSentByMap += (received.receiver -> (message :: messagesSentByMap.getOrElse(received.receiver, List())))})

    // the receiving actor processed one more message
    val processedBefore = actorProcessedMap.getOrElse(received.receiver, Set())
    actorProcessedMap += received.receiver -> (processedBefore + received)

    //println("\nNew messages after processing message: " + received)
    //sent.foreach(message => println(message + "  ==>  " + predecessors(message.id)))

    sent.map(message => (message.id, predecessors(message.id))).toMap
  }

  /**
    * Getter functions for the private maps
    */
  def actorProcessed(actor: ActorRef): Set[Message] = actorProcessedMap(actor)
  def actorCreatedBy(actor: ActorRef): ActorRef = actorCreatedByMap(actor)
  def actorCreatedIn(actor: ActorRef): Message = actorCreatedInMap(actor)
  def messagesSentBy(actor: ActorRef): List[Message] = messagesSentByMap(actor)

  def messageSendCausality(id: MessageId): Set[MessageId] = messageSendCausalityMap(id)
  def creatorCausality(id: MessageId): Set[MessageId] = creatorCausalityMap(id)
  def executedOnSenderCausality(id: MessageId): Set[MessageId] = executedOnSenderCausalityMap(id)
  def executedOnCreatorCausality(id: MessageId): Set[MessageId] = executedOnCreatorCausalityMap(id)
  def senderReceiverCausality(id: MessageId): Set[MessageId] = senderReceiverCausalityMap(id)

  def printMaps(): Unit = {
    print("\nActorCreatedBy: \n")
    actorCreatedByMap.foreach(x => println(x._1 + " -> " + x._2))
    print("\nActorCreatedIn: \n")
    actorCreatedInMap.foreach(x => println(x._1 + " -> " + x._2))
    print("\nActorProcessed: \n")
    actorProcessedMap.foreach(x => println(x._1 + " -> " + x._2))
    print("\nMessages sent by: \n")
    messagesSentByMap.foreach(x => println(x._1 + " -> " + x._2))
    print("\nSender-receiver: \n")
    senderReceiverCausalityMap.foreach(x => println(x._1 + " -> " + x._2))
  }
}