package akka.dispatch

import java.util.concurrent._

import akka.actor.{Actor, ActorCell, ActorInitializationException, ActorRef, ActorSystem, Cell, InternalActorRef, Props}
import akka.dispatch.state.ExecutionState
import akka.dispatch.state.Messages.{Message, MessageId}
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.sysmsg.{NoMessage, _}
import akka.event.Logging._
import akka.io.Tcp
import akka.io.Tcp._
import akka.pattern.PromiseActorRef
import com.typesafe.config.Config
import protocol.{AddedEvents, ErrorResponse}
import scheduler.{NOOptions, SchedulerOptions}
import scheduler.pctcp.{PCTCPStrategy, TaPCTCPStrategy}
import scheduler.pos.{DPOSStrategy, POSStrategy}
import scheduler.random.RandomWalkStrategy
import scheduler.rapos.RaposStrategy
import scheduler.user.UserInputStrategy
import time.TimerActor
import util.{CmdLineUtils, DispatcherUtils, FileUtils, ReflectionUtils}
import util.FunUtils._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
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

object TestingDispatcher {

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

  def awaitTermination(): Unit = actorSystem match {
    case Some(system) if system.dispatcher.isInstanceOf[TestingDispatcher] => Await.result(system.whenTerminated, Duration.Inf)
    case None => CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "Actor system for PCT Dispatcher is not set.")
  }

  /**
    * The following variables will be filled when the Dispatcher is set up with the actor system parameter
    */
  var actorSystem: Option[ActorSystem] = None
  var helperActor: Option[ActorRef] = None
  var timerActor: Option[ActorRef] = None
  var terminated = false

  /**
    * The dispatcher gets the next event to be scheduled via strategy
    */
  var strategy: SchedulingStrategy = UserInputStrategy

  var askPatternMessages: ListBuffer[(Cell, Envelope)] = ListBuffer()
  /**
    * Set the actor system used by the dispatcher
    * (Seperated from setUp method so that assigning the system and enabling the dispatcher can be done separately
    * e.g., when actor system is defined in a library but the dispatcher must be enabled after sending some messages)
    * @param system
    */
  def setActorSystem(system: ActorSystem): Unit = if (system.dispatcher.isInstanceOf[TestingDispatcher]) actorSystem = Some(system)

  /**
    * Enables dispatcher to deliver messages to the actors
    * Sets ioProvider to get inputs / write outputs
    * Is called by the app when it is done with the actor creation/initialization
    */
  def setUp(scheduler: String = DispatcherOptions.scheduler, schedulerOptions: SchedulerOptions = NOOptions): Unit = actorSystem match {
    // check to prevent initializing while using another Dispatcher type
    case Some(system) if system.dispatcher.isInstanceOf[TestingDispatcher] =>
      actorSystem = Some(system)
      helperActor = Some(system.actorOf(Props(new Actor() {
        override def receive: Receive = Actor.emptyBehavior
      }), "DispatcherHelperActor"))

      // create TimerActor only if the user uses virtual time (e.g. scheduler.schedule methods) in his program
      if(DispatcherOptions.useTimer) {
        val timer = system.actorOf(Props(new TimerActor(DispatcherOptions.timeStep, DispatcherOptions.maxNumTimeSteps)), "Timer")
        timerActor = Some(timer)
        timer ! AdvanceTime
      }

      scheduler.toUpperCase match {
        case "USERINPUT" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: Command line")
          strategy = UserInputStrategy
        case "PCTCP" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: PCTCP algorithm")
          strategy = new PCTCPStrategy(schedulerOptions)
        case "TAPCTCP" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: TAPCTCP algorithm")
          strategy = new TaPCTCPStrategy(schedulerOptions)
        case "POS" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: POS algorithm")
          strategy = new POSStrategy(schedulerOptions)
        case "DPOS" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: DPOS algorithm")
          strategy = new DPOSStrategy(schedulerOptions)
        case "RAPOS" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: RAPOS algorithm")
          strategy = new RaposStrategy(schedulerOptions)
        case "RANDOM" =>
          printLog(CmdLineUtils.LOG_INFO, "Input choice: Random walk")
          strategy = new RandomWalkStrategy(schedulerOptions)
        case _ =>
          printLog(CmdLineUtils.LOG_INFO, "Default input choice: Command line")
          strategy = UserInputStrategy
      }

      strategy.setUp(system)

    case _ => // do nth
  }


  def sendToDispatcher(msg: Any): Unit = helperActor match {
    case Some(actor) => actor ! msg
    case None => printLog(CmdLineUtils.LOG_WARNING, "Cannot send to the dispatcher, no helper actor is created")
  }

  def printLog(logType: Int, text: String): Unit = CmdLineUtils.printLog(logType, text)

  //todo take the message list as parameter
  val notInterceptedMsgs = Set(
    "OnHeaderWriteCompleted", "OnContentWriteCompleted", "OnStatusReceived", "OnHeadersReceived", "OnBodyPartReceived"
    // OnCompleted, OnThrowable
  )
}

/**
  * An extension of Dispatcher with todo logging facilities
  */
final class TestingDispatcher(_configurator: MessageDispatcherConfigurator,
                              _id: String,
                              _shutdownTimeout: FiniteDuration)
  extends Dispatcher(
    _configurator,
    _id,
    Int.MaxValue,
    Duration.Zero,
    CustomThreadPoolConfig(),
    _shutdownTimeout) {

  import TestingDispatcher._

  val state = new ExecutionState

  /**
    * Handler methods run synchronously by the dispatcher to handle requests
    */

  /**
    * Terminates the actor system
    */
  private def handleTerminate: Any = actorSystem match {
    case Some(system) if DispatcherOptions.willTerminate =>
      if (DispatcherOptions.logEventsToFile) logInfo()
      system.terminate
    case Some(system)  =>
      terminated = true // terminate is set but the system is not terminated
      printLog(CmdLineUtils.LOG_WARNING,  "System terminate request received. Not configured to force termination.")
      if (DispatcherOptions.logEventsToFile) logInfo()
    case None => printLog(CmdLineUtils.LOG_WARNING,  "Cannot terminate")
  }

  /**
    * @param message Message to be dispatched
    * @return list of generated events
    */
  private def handleDispatchMessageToActor(message: Message): Unit = state.existsActor(message.receiver) match {
    case Some(actor) if !state.isProcessed(message.id) =>
      state.updateState(MessageReceived(actor, message.id, message.envelope))
      // handle the actor message synchronously
      val receiver = actor.asInstanceOf[ActorCell]
      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, message.envelope)
      processMailbox(mbox)
    case Some(_) =>
      printLog(CmdLineUtils.LOG_ERROR, "The selected message is already processed: " + message)
    case None =>
      printLog(CmdLineUtils.LOG_WARNING, "Cannot dispatch message: " + message + " Recipient actor cannot be found.")
  }

  /**
    * @param message Message to be dropped
    * @return list of generated events
    */
  private def handleDropMessageFromActor(message: Message): Unit = state.existsActor(message.receiver) match {
    case Some(actor) if !state.isProcessed(message.id) =>
      state.updateState(MessageDropped(actor, message.id, message.envelope))
    case Some(actor) =>
      printLog(CmdLineUtils.LOG_ERROR, "The selected message is already processed: " + id)
    case None =>
      printLog(CmdLineUtils.LOG_WARNING, "Cannot drop message: " + message + " Recipient actor cannot be found.")
  }

  private def processMailbox(mbox: Mailbox): Unit = {
    if(mbox.hasMessages) { // Dispatcher runs this method after enqueuing a message
      ReflectionUtils.callPrivateMethod(mbox, "processAllSystemMessages")()
      ReflectionUtils.callPrivateMethod(mbox, "processMailbox")(1, 0L)
    } else {
      printLog(CmdLineUtils.LOG_ERROR, "Mailbox does not have any messages: " + mbox.messageQueue.numberOfMessages + "   " + mbox.messageQueue.toString)
    }
  }

  private def isActorReady(actor: Cell): Boolean =
    if(ReflectionUtils.readPrivateVal(actor, "behaviorStack").asInstanceOf[List[Actor.Receive]] == List.empty) {
      printLog(CmdLineUtils.LOG_DEBUG, "Actor behavior is not set. Cannot process mailbox. Will try again..")
      false
    } else true

  private def runOnExecutor(r: Runnable): Unit = {
    executorService execute r
  }

  private def sendProgramEvents() = runOnExecutor(toRunnable(() => {
    Thread.sleep(DispatcherOptions.networkDelay) // expect messages in response to the received message
    val events = state.collectEvents()
    printLog(CmdLineUtils.LOG_INFO, "Events: " + events)
    strategy.putResponse(AddedEvents(events, state.calculateDependencies(events)))
  }))

  // if a message is asked before its recipient actor is ready, askPatternMessages is nonempty
  // process them before collecting the events
  private def processAskMessages(): Unit = runOnExecutor(toRunnable(() => {
    val list = askPatternMessages.toList
    //todo revise for nested ask requests!
    if(list.nonEmpty) printLog(CmdLineUtils.LOG_DEBUG, "Handling ask messages sent when the actor was not ready... ")
    askPatternMessages = ListBuffer()
    list.foreach(x => {
      printLog(CmdLineUtils.LOG_DEBUG, "Handling ask pattern message to " + x._1.self.path + " " + x._2)
      executeAskMessageSync(x._1, x._2)
    })
    if(askPatternMessages.nonEmpty) {
      printLog(CmdLineUtils.LOG_DEBUG, "Handling MORE ask pattern messages... ")
      processAskMessages()
    }
  }))

  private def executeAskMessageSync(receiver: Cell, envelope: Envelope) = {
    if(isActorReady(receiver)) {
      printLog(CmdLineUtils.LOG_DEBUG, "Running synchronously: " + receiver.self.path + " " + envelope)
      val mbox = receiver.asInstanceOf[ActorCell].mailbox
      mbox.enqueue(receiver.self, envelope)
      val t = new Thread(toRunnable(() => processMailbox(mbox)))
      t.start()
      t.join(10000)
    } else {
      printLog(CmdLineUtils.LOG_DEBUG, "Actor not ready yet.. Saving for execution: " + receiver.self.path + " " + envelope)
      askPatternMessages += ((receiver, envelope))
      processAskMessages()
    }
  }

  /**
    * Overriden to intercept and keep the dispatched messages
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    if(terminated) { // if not forced for termination and more messages arrive
      CmdLineUtils.printLog(CmdLineUtils.LOG_ERROR, "More messages after PCTDispatcher is terminated:\n" + invocation)
      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, invocation)
      registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
      return
    }
    invocation match {
      // Handle Dispatcher messages
      case Envelope(msg, _) if msg.isInstanceOf[DispatcherMsg] =>  msg match {
        case DispatchMessageToActor(messageId) =>
          runOnExecutor(toRunnable(() => {
            if(state.existsMessage(messageId)){
              handleDispatchMessageToActor(state.getMessage(messageId))
              processAskMessages()
              sendProgramEvents()
            } else {
              printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
              strategy.putResponse(ErrorResponse("Message with id: " + messageId + " cannot be found."))
            }
          }))
          return
        case DropMessageFromActor(messageId) =>
          runOnExecutor(toRunnable(() => {
            if(state.existsMessage(messageId)) {
              handleDropMessageFromActor(state.getMessage(messageId))
              processAskMessages()
              sendProgramEvents()
            } else {
              printLog(CmdLineUtils.LOG_ERROR, "Message with id: " + messageId + " cannot be found.")
              strategy.putResponse(ErrorResponse("Message with id: " + messageId + " cannot be found."))
            }
          }))
          return
        case InitDispatcher =>
          // collect and send the initial program events
          printLog(CmdLineUtils.LOG_DEBUG, "The dispatcher sends the initial program events")
          processAskMessages()
          sendProgramEvents()
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

      // Do not intercept http connection messages
      case _ if notInterceptedMsgs.exists(x => invocation.message.toString.startsWith(x)) =>
        printLog(CmdLineUtils.LOG_DEBUG, "-- Message is configured not to be intercepted, scheduled. " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ if DispatcherOptions.noInterceptMsgs.exists(x => invocation.message.toString.startsWith(x)) =>
        printLog(CmdLineUtils.LOG_DEBUG, "Not intercepted: " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ =>
        // if the message is sent by the Ask Pattern:
        if (invocation.sender.isInstanceOf[PromiseActorRef]) {
          printLog(CmdLineUtils.LOG_INFO, "-- Message by AskPattern. Sending for for execution. " + receiver.self + " " + invocation)
          //checkAndWaitForActorBehavior(receiver)
          //val mbox = receiver.mailbox
          //mbox.enqueue(receiver.self, invocation)
          // registerForExecution posts the msg processing runnable to the thread pool executor
          // (It gets blocked since the only thread in the thread pool is possibly waiting on a future)
          // In case of ?, the msg is synchronously executed in the dispatcher thread

          // process the ask-pattern message synchronously (if the receiver actor is ready)
          // (NOTE: Message handlers in Akka should not have synchronized blocks by design)
          executeAskMessageSync(receiver, invocation)
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
        //if (!DispatcherUtils.isSystemActor(receiver.self))
        //  state.updateState(ActorDestroyed(receiver))

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
    super.shutdown
    System.exit(0)
  }

  def logInfo() = {
    FileUtils.printToFile("allEvents") { p =>
      state.getAllEvents.foreach(p.println)
    }

    val receivedMsgs = state.getAllEvents.filter(_.isInstanceOf[MessageReceived])
      .map(_.asInstanceOf[MessageReceived]).tail //remove the initial msg with empty sender/receiver

    FileUtils.printToFile("dependencies") { p => {
      state.getAllPredecessorsWithDepType.foreach(x => {
        val dependencies = x._2
        p.print(x._1 + " -> ")
        dependencies.foreach(d => p.print(d._2 + " "))
        p.println()
      })
      receivedMsgs.foreach( x =>
        p.println(x.id + "  Receiver: " + x.receiver.self.path.name + "  Sender: " + x.msg.sender.path.name + "  Payload: " + x.msg.message)
      )
    }}
  }
}

class TestingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new TestingDispatcher(
    this,
    config.getString("id"),
    Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}


// Send request of type CommandRequest to the dispatcher by:
// dispatcherServer ! request

