package akka.dispatch

import java.util.concurrent._

import akka.actor.{Actor, ActorCell, ActorInitializationException, ActorRef, ActorSystem, Cell, InternalActorRef, Props}
import akka.dispatch.io.{CmdLineIOProvider, IOProvider, PCTIOProvider, RandomExecProvider}
import akka.dispatch.state.ExecutionState
import akka.dispatch.state.Messages.{Message, MessageId}
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.sysmsg.{NoMessage, _}
import akka.event.Logging._
import akka.io.Tcp
import akka.io.Tcp._
import akka.pattern.PromiseActorRef
import com.typesafe.config.Config
import protocol.{ErrorResponse, MessagePredecessors}
import time.TimerActor
import util.{CmdLineUtils, DispatcherUtils, FileUtils, ReflectionUtils}
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
private case class DispatchMessageToActor(id: MessageId) extends DispatcherMsg

/**
  * To handle a request to drop the given message
  */
private case class DropMessageFromActor(id: MessageId) extends DispatcherMsg

object PCTDispatcher {

  /**
    * Called when the user requests to dispatch the given message to its recipient
    * @param messageId the message to dispatch
    */
  def dispatchMessage(messageId: MessageId): Unit = sendToDispatcher(DispatchMessageToActor(messageId))

  /**
    * Called when the user requests to drop the given message
    * @param messageId the message to drop
    */
  def dropMessage(messageId: MessageId): Unit = sendToDispatcher(DropMessageFromActor(messageId))

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
    * Set the actor system used by the dispatcher
    * (Seperated from setUp method so that assigning the system and enabling the dispatcher can be done separately
    * e.g., when actor system is defined in a library but the dispatcher must be enabled after sending some messages)
    * @param system
    */
  def setActorSystem(system: ActorSystem): Unit = actorSystem = Some(system)

  /**
    * Enables dispatcher to deliver messages to the actors
    * Sets ioProvider to get inputs / write outputs
    * Is called by the app when it is done with the actor creation/initialization
    */
  def setUp(): Unit = actorSystem match {
      // check to prevent initializing while using another Dispatcher type
      case Some(system) if system.dispatcher.isInstanceOf[PCTDispatcher] =>
        actorSystem = Some(system)
        helperActor = Some(system.actorOf(Props(new Actor() {
          override def receive: Receive = Actor.emptyBehavior
        }), "DispatcherHelperActor"))

        // create TimerActor only if the user uses virtual time (e.g. scheduler.schedule methods) in his program
        if(DispatcherOptions.useTimer) {
          val timer = system.actorOf(Props(new TimerActor(DispatcherOptions.timeStep)), "Timer")
          timerActor = Some(timer)
          timer ! AdvanceTime
        }

        DispatcherOptions.uiChoice.toUpperCase match {
          case "CMDLINE" =>
            printLog(CmdLineUtils.LOG_INFO, "Input choice: Command line")
            ioProvider = CmdLineIOProvider
          case "PCT" =>
            printLog(CmdLineUtils.LOG_INFO, "Input choice: PCT algorithm")
            ioProvider = PCTIOProvider
          case "RANDOM" =>
            printLog(CmdLineUtils.LOG_INFO, "Input choice: Naive random")
            ioProvider = RandomExecProvider
          case _ =>
            printLog(CmdLineUtils.LOG_INFO, "Default input choice: Command line")
            ioProvider = CmdLineIOProvider
        }

        ioProvider.setUp(system)

      case _ => // do nth
  }

  def sendToDispatcher(msg: Any): Unit = helperActor match {
    case Some(actor) => actor ! msg
    case None => printLog(CmdLineUtils.LOG_WARNING, "Cannot send to the dispatcher, no helper actor is created")
  }

  def printLog(logType: Int, text: String): Unit = CmdLineUtils.printLog(logType, text)

  //def getAllMessagesIds: Set[MessageId] = state.getAllMessagesIds

  //def getAllActorMessagesToProcess: Map[ActorRef, Set[Message]] = state.getAllActorMessagesToProcess

  //def getAllPredecessors: Set[(MessageId, Set[MessageId])] = state.getAllPredecessors
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

  val state = new ExecutionState

  /**
    * Handler methods run synchronously by the dispatcher to handle requests
    */

  /**
    * Prepends an initial MessageReceived event to the first list of events
    * (which not produced in response to any actor message)
    * @return list of events generated in the initialization
    */
  private def handleInitiate(): List[(MessageId, ProgramEvent)] = state.collectEvents()

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
  def handleDispatchMessageToActor(message: Message): List[(MessageId, ProgramEvent)] = state.existsActor(message.receiver) match {
    case Some(actor) if !state.isProcessed(message.id) =>
      state.updateState(MessageReceived(actor, message.id, message.envelope))
      // handle the actor message synchronously
      val receiver = actor.asInstanceOf[ActorCell]
      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, message.envelope)
      processMailbox(mbox)
      state.collectEvents()
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
  def handleDropMessageFromActor(message: Message): List[(MessageId, ProgramEvent)] = state.existsActor(message.receiver) match {
    case Some(actor) if !state.isProcessed(message.id) =>
      state.updateState(MessageDropped(actor, message.id, message.envelope))
      state.collectEvents()
    case Some(actor) =>
      printLog(CmdLineUtils.LOG_ERROR, "The selected message is already processed: " + id)
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
    * Overriden to intercept and keep the dispatched messages
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {

    invocation match {
      // Handle Dispatcher messages
      case Envelope(msg, _) if msg.isInstanceOf[DispatcherMsg] =>  msg match {
        case DispatchMessageToActor(messageId) =>
          runOnExecutor(toRunnable(() => {
            if(state.existsMessage(messageId)){
              val events = handleDispatchMessageToActor(state.getMessage(messageId))
              ioProvider.putResponse(MessagePredecessors(state.calculateDependencies(events)))
            } else {
              printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
              ioProvider.putResponse(ErrorResponse("Message with id: " + messageId + " cannot be found."))
            }
          }))
          return
        case DropMessageFromActor(messageId) =>
          runOnExecutor(toRunnable(() => {
            if(state.existsMessage(messageId)) {
              val events = handleDropMessageFromActor(state.getMessage(messageId))
              ioProvider.putResponse(MessagePredecessors(state.calculateDependencies(events)))
            } else {
              printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
              ioProvider.putResponse(ErrorResponse("Message with id: " + messageId + " cannot be found."))
            }
          }))
          return
        case InitDispatcher =>
          runOnExecutor(toRunnable(() => {
            val events = handleInitiate()
            ioProvider.putResponse(MessagePredecessors(state.calculateDependencies(events)))
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
    state.updateState(MessageSent(receiver, invocation))

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
        if (!DispatcherUtils.isSystemActor(receiver.self))
          state.updateState(ActorDestroyed(receiver))

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
    if (!DispatcherUtils.isSystemActor(actor.self) /*&& !actor.self.toString().startsWith("Actor[akka://" + systemName + "/user/" + dispatcherInitActorName)*/ )
      state.updateState(ActorCreated(actor))

    new Mailbox(mailboxType.create(Some(actor.self), Some(actor.system))) with DefaultSystemMessageQueue
  }

  /**
    * Overriden to output the recorded events
    */
  override def shutdown: Unit = {
    printLog(CmdLineUtils.LOG_INFO, "Shutting down.. ")

    FileUtils.printToFile("allEvents") { p =>
      state.getAllEvents.foreach(p.println)
    }
    
    FileUtils.printToFile("allMessages") { p =>
      state.getAllEvents.filter(_.isInstanceOf[MessageReceived]).foreach(p.println)
    }

    FileUtils.printToFile("dependencies") { p =>
      state.getAllPredecessorsWithDepType.foreach(x => {
        val dependencies = x._2
        p.print(x._1 + " -> ")
        dependencies.foreach(d => p.print(d._2 + " "))
        p.println()
      })

    }
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

