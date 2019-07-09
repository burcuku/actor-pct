package akka.dispatch.state

import akka.actor.{ActorRef, Cell}
import akka.dispatch.TestingDispatcher.{Message, printLog}
import akka.dispatch._
import akka.dispatch.state.DependencyGraphBuilder.Dependency
import akka.dispatch.util.{CmdLineUtils, ReflectionUtils}
import explorer.protocol.MessageId

class ExecutionState {

  private var messages: Messages = new Messages()

  /**
    * Keeps the messages sent to the actors - the messages are not delivered immediately but collected here
    */
  private val actorMessagesMap: ActorMessagesMap = new ActorMessagesMap(messages)
  private val eventBuffer: EventBuffer = new EventBuffer()
  private var processed: Set[MessageId] = Set()

  // add an initial message when dispatcher is initialized
  // before the events generated in the beginning (not in response to the receipt of a message)
  eventBuffer.addEvent(0L, MessageReceived(/*no cell*/null, 0L, ReflectionUtils.createNewEnvelope("", ActorRef.noSender)))
  messages.addMessage(ActorRef.noSender, ReflectionUtils.createNewEnvelope("", ActorRef.noSender))

  private val dependencyGraphBuilder = new DependencyGraphBuilder()

  /**
    * Calculate the dependencies of the messages generated in response to a received message
    * @param list of events generated in response to a message
    * @return set of dependencies (i.e. predecessors in the sense of causality of the messages) of each generated message
    */
  def calculateDependencies(list: List[(MessageId, InternalProgramEvent)]): Map[MessageId, Set[MessageId]] = list match {
    case Nil => Map()
    case x :: xs if x._2.isInstanceOf[MessageReceived] =>
      val receivedMessage = getMessage(list.head._1) // the first event is always of type MESSAGE_RECEIVED
      val sentMessages = list.filter(_._2.isInstanceOf[MessageSent]).map(e => getMessage(e._1)) // ids of the sent messages
      val createdActors = list.filter(_._2.isInstanceOf[ActorCreated]).map(_._2.asInstanceOf[ActorCreated].actor.self) // created actor refs
      calculateDependencies(receivedMessage, sentMessages, createdActors)
    case _ =>
      // The event sequence must start with an instance of MessageReceived if it is non-empty
      printLog(CmdLineUtils.LOG_ERROR, "Unexpected event sequence generated in response to a message.")
      Map() // do nth
  }

  def calculateDependencies(received: Message, sent: List[Message], created: List[ActorRef]): Map[MessageId, Set[MessageId]] =
    dependencyGraphBuilder.calculateDependencies(received, sent, created).map(pair => (pair._1, pair._2.map(dep => dep._2)))

  def updateState(event: InternalProgramEvent): Any = event match {

    case MessageSent(receiver, invocation) =>
      val messageId = messages.addMessage(receiver.self, invocation)
      actorMessagesMap.addMessage(receiver, messageId)
      eventBuffer.addEvent(messageId, event)

    case MessageReceived(receiver, id, invocation) if !processed.contains(id) =>
      eventBuffer.addEvent(id, MessageReceived(receiver, id, invocation))
      processed += id

    case MessageDropped(receiver, id, invocation) if !processed.contains(id) =>
      eventBuffer.addEvent(id, MessageDropped(receiver, id, invocation))
      processed += id

    case ActorCreated(receiver) =>
      eventBuffer.addEvent(ActorCreated(receiver))
      actorMessagesMap.addActor(receiver)

    case ActorDestroyed(receiver) =>
      eventBuffer.addEvent(ActorDestroyed(receiver))
      actorMessagesMap.removeActor(receiver)
  }

  def collectEvents(): List[(MessageId, InternalProgramEvent)] = eventBuffer.consumeEvents

  def existsActor(actor: ActorRef): Option[Cell] = actorMessagesMap.hasActor(actor)

  def isProcessed(id: MessageId): Boolean = processed(id)

  def isEnabled(id: MessageId): Boolean = getPredecessors(id).forall(x => processed.contains(x))

  def getMessage(id: MessageId): Message = messages.get(id)

  def getAllMessagesIds: Set[MessageId] = messages.getAllMessageIds

  def getAllMessages: List[Message] = messages.getAllMessages.toList.sortBy(_.id)

  def getAllMessages(actor: Cell): Option[List[MessageId]] = actorMessagesMap.getAllMessages(actor)

  def getActorMessages(actor: ActorRef): Set[Message] = messages.getAllMessages.filter(m => m.receiver == actor).toSet

  def getActorMessagesToProcess(actor: ActorRef): Set[Message] = messages.getAllMessages.filter(m => m.receiver == actor && !processed.contains(m.id)).toSet

  def getAllActorMessages: Map[ActorRef, Set[Message]] = actorMessagesMap.getAllActors.map(a => (a.self, getActorMessages(a.self))).toMap

  def getAllActorMessagesToProcess: Map[ActorRef, Set[Message]] = actorMessagesMap.getAllActors.map(a => (a.self, getActorMessagesToProcess(a.self))).toMap

  def existsMessage(id: MessageId): Boolean = messages.getAllMessageIds.contains(id)

  def getPredecessors(messageId: MessageId): Set[MessageId] = dependencyGraphBuilder.predecessors(messageId).map(dep => dep._2)

  def getAllPredecessors: Set[(MessageId, Set[MessageId])] = messages.getAllMessageIds.map(id => (id, dependencyGraphBuilder.predecessors(id)))
    .map(pair => (pair._1, pair._2.map(dep => dep._2)))

  def getAllPredecessorsWithDepType: Set[(MessageId, Set[Dependency])] = messages.getAllMessageIds.map(id => (id, dependencyGraphBuilder.predecessors(id)))

  def getAllEvents: List[InternalProgramEvent] = eventBuffer.getAllEvents

  val random = scala.util.Random
}
