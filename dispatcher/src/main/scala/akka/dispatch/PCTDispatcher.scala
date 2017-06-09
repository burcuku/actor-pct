package akka.dispatch

import java.util.concurrent._

import akka.actor.{Actor, ActorCell, ActorInitializationException, ActorRef, ActorSystem, Cell, InternalActorRef, Props}
import akka.dispatch.PCTDispatcher.Message
import akka.dispatch.io.{CmdLineIOProvider, IOProvider, PCTIOProvider}
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.sysmsg.{NoMessage, _}
import akka.event.Logging._
import akka.io.Tcp
import akka.io.Tcp._
import akka.pattern.PromiseActorRef
import com.typesafe.config.{Config, ConfigFactory}
import time.TimerActor
import protocol.{Event, Events, _}
import util.{CmdLineUtils, FileUtils, IdGenerator, ReflectionUtils}
import util.FunUtils._

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * The following messages used to communicate the user requests to the dispatcher
  * These messages are not intercepted or delivered to any actor
  * They are used to invoke associated handler methods async on the dispatcher thread
  */
sealed trait DispatcherMsg

/**
  * To handle request to start
  */
private case object InitDispatcher extends DispatcherMsg

/**
  * To handle request to terminate
  */
private case object EndDispatcher extends DispatcherMsg

/**
  * To handle PCT request to dispatch the given message to its recipient actor
  */
private case class DispatchMessageToActor(message: Message) extends DispatcherMsg

/**
  * To handle a request to drop the given message
  */
private case class DropMessageFromActor(message: Message) extends DispatcherMsg

object PCTDispatcher {

  type MessageId = Long
  case class Message(id: MessageId, receiver: ActorRef, msg: Envelope)

  /**
    * Keeps the messages sent to the actors - the messages are not delivered immediately but collected here
    */
  private val actorMessagesMap: ActorMessagesMap = new ActorMessagesMap() //todo revise - might not be needed for PCTDispatcher
  private val eventBuffer: EventBuffer = new EventBuffer()
  private var messages: Map[MessageId, Message] = Map()
  private var processed: Set[MessageId] = Set()
  private val idGenerator = new IdGenerator(1)

  // add an initial message when dispatcher is initialized
  // before the events generated in the beginning (not in response to the receipt of a message)
  eventBuffer.addEvent(0L, MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender)))
  messages += (0L -> Message(0L, ActorRef.noSender, Envelope("", ActorRef.noSender)))
  processed += 0L

  private val dependencyGraphBuilder = new DependencyGraphBuilder()

  /**
    * Called when the user requests to dispatch the given message to its recipient
    * @param messageId the message to dispatch
    */
  def dispatchMessage(messageId: MessageId): Unit = messages.get(messageId) match {
    case Some(m) => sendToDispatcher(DispatchMessageToActor(m))
    case None => printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
  }

  /**
    * Called when the user requests to drop the given message
    * @param messageId the message to drop
    */
  def dropMessage(messageId: MessageId): Unit = messages.get(messageId) match {
    case Some(m) => sendToDispatcher(DropMessageFromActor(m))
    case None => printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
  }

  /**
    * Called when the user requests to initiate the dispatcher
    * Send the initial list of actor events to the user
    */
  def initiateDispatcher(): Unit = sendToDispatcher(InitDispatcher)

  /**
    * Called when the user requests to terminate the program
    */
  def terminateDispatcher(): Unit = sendToDispatcher(EndDispatcher)

  /**
    * The following variables will be filled when the Dispatcher is set up with the actor system parameter
    */
  var actorSystem: Option[ActorSystem] = None
  var helperActor: Option[ActorRef] = None
  var timerActor: Option[ActorRef] = None

  /**
    * The dispatcher gets user/algorithm inputs via ioProvider
    */
  var ioProvider: IOProvider = CmdLineIOProvider

  /**
    * Enables dispatcher to deliver messages to the actors
    * Sets ioProvider to get inputs / write outputs
    * Is called by the app when it is done with the actor creation/initialization
    *
    * @param system Actor System
    */
  // todo reorganize reading configs
  def setUp(system: ActorSystem): Unit = {
    system.dispatcher match { // check to prevent initializing while using another Dispatcher type
      case d: PCTDispatcher =>
        actorSystem = Some(system)
        helperActor = Some(system.actorOf(Props(new Actor() {
          override def receive: Receive = Actor.emptyBehavior
        }), "DispatcherHelperActor"))

        // read the virtual time step config and create TimerActor
        var timeStep: FiniteDuration = FiniteDuration(1, TimeUnit.MILLISECONDS)

        // create TimerActor only if the user uses virtual time (e.g. scheduler.schedule methods) in his program
        val useTimer: Boolean = try {
          ConfigFactory.load(DispatcherUtils.dispatcherConfigFile).getBoolean("pct-dispatcher.useVirtualTimer")
        } catch {
          case e: Exception =>
            true
        }

        if (useTimer) {
          val timer = system.actorOf(Props(new TimerActor(timeStep)), "Timer")
          timerActor = Some(timer)

          try {
            FiniteDuration(ConfigFactory.load(DispatcherUtils.dispatcherConfigFile).getDuration("pct-dispatcher.timestep", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
          } catch {
            case e: Exception =>
              printLog(CmdLineUtils.LOG_WARNING, "No valid timestep duration provided in configuration file. Using default timestep 1 MILLISECONDS")
          }
          timer ! AdvanceTime
        }

        try {
          ConfigFactory.load(DispatcherUtils.dispatcherConfigFile).getString("pct-dispatcher.inputChoice").toUpperCase match {
            case "CMDLINE" =>
              printLog(CmdLineUtils.LOG_INFO, "Input choice: Command line")
              ioProvider = CmdLineIOProvider
            case "PCT" =>
              printLog(CmdLineUtils.LOG_INFO, "Input choice: PCT algorithm")
              ioProvider = PCTIOProvider
            case _ => printLog(CmdLineUtils.LOG_ERROR, "Input choice is not provided in the configuration file." +
              "\nUsing command line by default")
          }
        } catch {
          case e: Exception => printLog(CmdLineUtils.LOG_ERROR, "Input choice configuration cannot be read. Using command line by default")
        }

        ioProvider.setUp(system)

      case _ => // do nth
    }
  }

  def sendToDispatcher(msg: Any): Unit = helperActor match {
    case Some(actor) => actor ! msg
    case None => printLog(CmdLineUtils.LOG_WARNING, "Cannot send to the dispatcher, no helper actor is created")
  }

  def printLog(logType: Int, text: String): Unit = CmdLineUtils.printLog(logType, text)

  def getAllMessagesIds: Set[MessageId] = messages.keySet

  def getAllMessages: List[Message] = messages.values.toList.sortBy(_.id)

  def getActorMessages(actor: ActorRef): Set[Message] = messages.values.filter(m => m.receiver == actor).toSet

  def getActorMessagesToProcess(actor: ActorRef): Set[Message] = messages.values.filter(m => m.receiver == actor && !processed.contains(m.id)).toSet

  def getAllActorMessages: Map[ActorRef, Set[Message]] = actorMessagesMap.getAllActors.map(a => (a.self, getActorMessages(a.self))).toMap

  def getAllActorMessagesToProcess: Map[ActorRef, Set[Message]] = actorMessagesMap.getAllActors.map(a => (a.self, getActorMessagesToProcess(a.self))).toMap

  def getPredecessors(messageId: MessageId): Set[MessageId] = dependencyGraphBuilder.predecessors(messageId)

  def getAllPredecessors: Set[(MessageId, Set[MessageId])] = messages.keySet.map(id => (id, dependencyGraphBuilder.predecessors(id)))
}

/**
  * An extension of Dispatcher with todo logging facilities
  */
final class PCTDispatcher(_configurator: MessageDispatcherConfigurator,
                                _id: String,
                                _shutdownTimeout: FiniteDuration)
  extends Dispatcher(
    _configurator,
    _id,
    Int.MaxValue,
    Duration.Zero,
    PCTThreadPoolConfig(),
    _shutdownTimeout) {

  import PCTDispatcher._


  /**
    * Handler methods run synchronously by the dispatcher to handle requests
    */

  /**
    * Prepends an initial MessageReceived event to the first list of events
    * (which not produced in response to any actor message)
    * @return list of events generated in the initialization
    */
  private def handleInitiate(): List[(MessageId, Event)] = eventBuffer.consumeEvents

  /**
    * Terminates the actor system
    */
  private def handleTerminate: Any = actorSystem match {
    case Some(system) => system.terminate
    case None => printLog(CmdLineUtils.LOG_WARNING,  "Cannot terminate")
  }

  /**
    * @param message Message to be dispatched
    * @return list of generated events
    */
  def handleDispatchMessageToActor(message: Message): List[(MessageId, Event)] = actorMessagesMap.hasActor(message.receiver) match {
    case Some(actor) if !processed.contains(message.id) =>
      eventBuffer.addEvent(message.id, MessageReceived(actor.self, message.msg))
      processed += message.id
      // handle the actor message synchronously
      val receiver = actor.asInstanceOf[ActorCell]
      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, message.msg)
      processMailbox(mbox)
      eventBuffer.consumeEvents
    case Some(_) =>
      printLog(CmdLineUtils.LOG_ERROR, "The selected message is already processed: " + message)
      List()
    case None =>
      printLog(CmdLineUtils.LOG_WARNING, "Cannot dispatch message: " + message + " Recipient actor cannot be found.")
      List()
  }


  /**
    * @param message Message to be dropped
    * @return list of generated events
    */
  def handleDropMessageFromActor(message: Message): List[(MessageId, Event)] = actorMessagesMap.hasActor(message.receiver) match {
    case Some(actor) if !processed.contains(message.id) =>
      eventBuffer.addEvent(message.id, MessageDropped(actor.self, message.msg))
      processed += message.id
      // handle the actor message synchronously
      eventBuffer.consumeEvents
    case Some(_) =>
      printLog(CmdLineUtils.LOG_ERROR, "The selected message is already processed: " + message)
      List()
    case None =>
      printLog(CmdLineUtils.LOG_WARNING, "Cannot drop message: " + message + " Recipient actor cannot be found.")
      List()
  }

  private def processMailbox(mbox: Mailbox): Unit = {
    if(mbox.hasMessages) { // DebuggerDispatcher runs this method after enqueuing a message
      ReflectionUtils.callPrivateMethod(mbox, "processAllSystemMessages")()
      ReflectionUtils.callPrivateMethod(mbox, "processMailbox")(1, 0L)
    } else {
      printLog(CmdLineUtils.LOG_ERROR, "Mailbox does not have any messages: " + mbox.messageQueue.numberOfMessages + "   " + mbox.messageQueue.toString)
    }
  }

  private def checkAndWaitForActorBehavior(actor: ActorCell): Unit = {
    if(ReflectionUtils.readPrivateVal(actor, "behaviorStack").asInstanceOf[List[Actor.Receive]] == List.empty) {
      printLog(CmdLineUtils.LOG_DEBUG, "Actor behavior is not set. Cannot process mailbox. Trying again..")
      // We use blocking wait since the ? pattern is run synchronously
      // and the thread dispatching the messages are blocked until this mailbox is processed
      Thread.sleep(500)
      checkAndWaitForActorBehavior(actor)
    }
  }

  private def runOnExecutor(r: Runnable): Unit = {
    executorService execute r
  }

  /**
    * Calculate the dependencies of the messages generated in response to a received message
    * @param list of events generated in response to a message
    * @return set of dependencies (i.e. predecessors in the sense of causality of the messages) of each generated message
    */
  def calculateDependencies(list: List[(MessageId, Event)]): Map[MessageId, Set[MessageId]] = list match {
    case Nil => Map()
    case x :: xs if x._2.isInstanceOf[MessageReceived] =>
      val receivedMessage = messages(list.head._1) // the first event is always of type MESSAGE_RECEIVED
      val sentMessages = list.filter(_._2.isInstanceOf[MessageSent]).map(e => messages(e._1)) // ids of the sent messages
      val createdActors = list.filter(_._2.isInstanceOf[ActorCreated]).map(_._2.asInstanceOf[ActorCreated].actor) // created actor refs
      dependencyGraphBuilder.calculateDependencies(receivedMessage, sentMessages, createdActors)
    case _ =>
      // The event sequence must start with an instance of MessageReceived if it is non-empty
      printLog(CmdLineUtils.LOG_ERROR, "Unexpected event sequence generated in response to a message.")
      Map() // do nth
  }

  /**
    * Overriden to intercept and keep the dispatched messages
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {

    invocation match {
      // Handle Dispatcher messages
      case Envelope(msg, _) if msg.isInstanceOf[DispatcherMsg] =>  msg match {
        case DispatchMessageToActor(message) =>
          runOnExecutor(toRunnable(() => {
            val events = handleDispatchMessageToActor(message)
            ioProvider.putResponse(MessagePredecessors(calculateDependencies(events)))
          }))
          return
        case DropMessageFromActor(message) =>
          runOnExecutor(toRunnable(() => {
            val events = handleDropMessageFromActor(message)
            ioProvider.putResponse(MessagePredecessors(calculateDependencies(events)))
          }))
          return
        case InitDispatcher =>
          runOnExecutor(toRunnable(() => {
            val events = handleInitiate()
            ioProvider.putResponse(MessagePredecessors(calculateDependencies(events)))
          }))
          return
        case EndDispatcher =>
          runOnExecutor(toRunnable(() => {
            handleTerminate
          }))
          return
      }

      // Do not intercept the log messages
      case Envelope(Error(_, _, _, _), _)
           | Envelope(Warning(_, _, _), _)
           | Envelope(Info(_, _, _), _)
           | Envelope(Debug(_, _, _), _) =>
        printLog(CmdLineUtils.LOG_DEBUG, "Log msg is delivered. Running synchronously. " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        //registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        // Instead of posting the msg handler runnable, synchronously run it (the msg handler just does logging)
        processMailbox(mbox)
        return

      // Do not intercept the messages sent to the system actors
      case _ if DispatcherUtils.isSystemActor(receiver.self) =>
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      // Do not intercept Tcp connection internal messages
      case Envelope(Tcp.Register(_, _, _), _)
           | Envelope(Bound(_), _)
           | Envelope(Connected(_, _), _)
           | Envelope(CommandFailed(_), _)
           | Envelope(Received(_), _) =>
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ =>
        // if the message is sent by the Ask Pattern:
        if (invocation.sender.isInstanceOf[PromiseActorRef]) {
          printLog(CmdLineUtils.LOG_DEBUG, "-- Message by AskPattern. Sending for execution. " + receiver.self + " " + invocation)
          checkAndWaitForActorBehavior(receiver)
          val mbox = receiver.mailbox
          mbox.enqueue(receiver.self, invocation)
          // registerForExecution posts the msg processing runnable to the thread pool executor
          // (It gets blocked since the only thread in the thread pool is possibly waiting on a future)
          // In case of ?, the msg is synchronously executed in the dispatcher thread

          // (NOTE: Message handlers in Akka should not have synchronized blocks by design)
          val t = new Thread(toRunnable(() => processMailbox(mbox)))
          t.start()
          t.join(10000)
          return
        }

      // Go with the default execution, intercept and record the message
    }

    printLog(CmdLineUtils.LOG_INFO, "Intercepting msg to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)

    // Add the intercepted message to the messages map
    val messageId = idGenerator.next
    messages += (messageId -> Message(messageId, receiver.self, invocation))

    // Add the intercepted message into the list of output events
    eventBuffer.addEvent(messageId, MessageSent(receiver.self, invocation))

    // Add the intercepted message into intercepted messages map //todo revise - may not be needed for PCTDispatcher
    actorMessagesMap.addMessage(receiver, invocation)

    //// Commented out the following original code to block default enqueue to the actor's mailbox
    //// mbox.enqueue(receiver.self, invocation)
    //// registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
  }

  /**
    * Overriden to update the actorMap with termination and other system messages
    *
    * @param receiver   receiver of the system message
    * @param invocation the dispatched system message
    */
  override def systemDispatch(receiver: ActorCell, invocation: SystemMessage): Unit = {
    printLog(CmdLineUtils.LOG_DEBUG, "Delivered system msg: " + invocation + "   Actor: " + receiver.self)

    // run the updates on the thread pool thread
    executorService execute updateActorMapWithSystemMessage(receiver, invocation)

    // System messages are processed each time when the mailbox is executed, their execution are not controlled
    val mbox = receiver.mailbox
    mbox.systemEnqueue(receiver.self, invocation)
    registerForExecution(mbox, hasMessageHint = false, hasSystemMessageHint = true) // terminating an actor..
  }

  def updateActorMapWithSystemMessage(receiver: ActorCell, invocation: SystemMessage): Runnable = toRunnable(() => {
    /**
      * Create messages are directly enqueued into the mailbox, without calling systemDispatch
      * IMPORTANT: Run here before registerForExecution so that actorMessagesMap is updated before terminated actor is deleted
      */
    invocation match {
      case Create(failure: Option[ActorInitializationException]) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Create by failure: " + failure)
      case Recreate(cause: Throwable) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Recreate by cause: " + cause)
      case Suspend() => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Suspend")
      case Resume(causedByFailure: Throwable) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Resume by failure: " + causedByFailure)
      case Terminate() =>
        CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg terminates: " + receiver.self)
        // add to the event list only if it is not a system actor
        if (!DispatcherUtils.isSystemActor(receiver.self)) {
          eventBuffer.addEvent(ActorDestroyed(receiver.self))
          actorMessagesMap.removeActor(receiver)
        }
      case Supervise(child: ActorRef, async: Boolean) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Supervise. Child: " + child)
      case Watch(watchee: InternalActorRef, watcher: InternalActorRef) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Watch. Watchee: " + watchee + " Watcher: " + watcher)
      case Unwatch(watchee: ActorRef, watcher: ActorRef) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Unwatch. Watchee: " + watchee + " Watcher: " + watcher)
      case NoMessage => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: NoMessage")
      case _ => // do not track the other system messages for now
    }
  })

  /**
    * Overriden to add the actors with the created mailbox into the ActorMap
    */
  override def createMailbox(actor: akka.actor.Cell, mailboxType: MailboxType): Mailbox = {
    printLog(CmdLineUtils.LOG_DEBUG, "Created mailbox for: " + actor.self + " in thread: " + Thread.currentThread().getName)

    // add to the event list only if it is not a system actor
    if (!DispatcherUtils.isSystemActor(actor.self) /*&& !actor.self.toString().startsWith("Actor[akka://" + systemName + "/user/" + dispatcherInitActorName)*/ ) {
      eventBuffer.addEvent(ActorCreated(actor.self))
      actorMessagesMap.addActor(actor)
    }

    new Mailbox(mailboxType.create(Some(actor.self), Some(actor.system))) with DefaultSystemMessageQueue
  }

  /**
    * Overriden to output the recorded events
    */
  override def shutdown: Unit = {
    printLog(CmdLineUtils.LOG_INFO, "Shutting down.. ")

    FileUtils.printToFile("allEvents") { p =>
      eventBuffer.getAllEvents.foreach(p.println)
    }
    
    FileUtils.printToFile("allMessages") { p =>
      eventBuffer.getEvents[MessageReceived].foreach(p.println)      
    }
    //getAllMessages.foreach(println)

    super.shutdown
  }
}

class PCTDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new PCTDispatcher(
    this,
    config.getString("id"),
    Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}

