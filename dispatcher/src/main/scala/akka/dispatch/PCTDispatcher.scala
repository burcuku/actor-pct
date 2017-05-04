package akka.dispatch

import java.util.concurrent._

import akka.actor.{Actor, ActorCell, ActorInitializationException, ActorRef, ActorSystem, Cell, InternalActorRef, Props}
import akka.dispatch.io._
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.sysmsg.{NoMessage, _}
import akka.event.Logging._
import akka.io.Tcp
import akka.io.Tcp.{apply => _, _}
import akka.pattern.{PromiseActorRef, ask}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import time.{MockTime, TimerActor}
import protocol.{Event, _}
import util.{CmdLineUtils, FileUtils, ReflectionUtils}
import util.FunUtils._

import scala.collection.mutable.{Set => MSet}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * The following messages used to communicate the user requests to the dispatcher
  * These messages are not intercepted or delivered to any actor
  * They are used to invoke associated handler methods async on the dispatcher thread
  */
sealed trait DispatcherMsg

/**
  * To handle ActionRequest Init
  */
private case object InitDispatcher extends DispatcherMsg

/**
  * To handle ActionRequest End
  */
private case object EndDispatcher extends DispatcherMsg

/**
  * To handle ActionRequest to dispatch to a given actor
  */
private case class DispatchToActor(actor: Cell) extends DispatcherMsg

/**
  * To handle ActionRequest to dispatch to the next actor in the execution trace
  */
private case object DispatchToNextActor extends DispatcherMsg

/**
  * To handle DropMsgRequest to drop the message at the head of the actor msg list
  */
private case class DropActorMsg(actor: Cell) extends DispatcherMsg

/**
  * For sending log messages to the dispatcher generated while reading a user Request
  */
private case class LogMsg(logType: Int, text: String) extends DispatcherMsg

object PCTDispatcher {
  /**
    * The dispatcher gets user/algorithm inputs via ioProvider
    */
  val ioProvider: IOProvider = CmdLineIOProvider

  /**
    * Keeps the messages sent to the actors - the messages are not delivered immediately but collected here
    */
  private val actorMessagesMap: ActorMessagesMap = new ActorMessagesMap()

  /**
    * Called when the user requests to dispatch the next message to a given actor
    *
    * @param actor cell which receives the next message
    */
  def dispatchToActor(actor: Cell): Unit = {
    sendToDispatcher(DispatchToActor(actor))
  }

  /**
    * Called when the user requests to dispatch the next message to a given actor
    *
    * @param actorId actor name which receives the next message
    */
  def dispatchToActor(actorId: String): Unit = {
    val actor = actorMessagesMap.getActor(actorId)
    actor match {
      case Some(a) => dispatchToActor(a)
      case None =>
        println(listOfActorsByRef)
        printDispatcherLog(Events.LOG_WARNING, "Cannot dispatch to actor: " + actorId + " No such actor.")
    }
  }

  /**
    * Called when the user requests to step next in the replayed execution trace
    * Dispatch message to the next actor in replayed trace
    */
  def dispatchToNextActor(): Unit = {
    sendToDispatcher(DispatchToNextActor)
  }

  /**
    * Called when the user requests to drop the head message of the given actor
    *
    * @param actor cell whose head message will be dropped
    */
  def dropActorMsg(actor: Cell): Unit = {
    sendToDispatcher(DropActorMsg(actor))
  }

  /**
    * Called when the user requests to drop the head message of the given actor
    *
    * @param actorId cell whose head message will be dropped
    */
  def dropActorMsg(actorId: String): Unit = {
    val actor = actorMessagesMap.getActor(actorId)
    actor match {
      case Some(a) => dropActorMsg(a)
      case None => printDispatcherLog(Events.LOG_WARNING, "Cannot drop msg from the actor: " + actorId + " No such actor.")
    }
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

  val askTimeOutMsec: Int = 5000 // used when the internal state of an actor is asked

  /**
    * Enables dispatcher to deliver messages to the actors
    * To be called by the app when it is done with the actor creation/initialization
    *
    * @param system Actor System
    */
  def setUp(system: ActorSystem): Unit = {
    system.dispatcher match { // check to prevent initializing ioProvider while using another Dispatcher type
      case d: PCTDispatcher =>
        actorSystem = Some(system)
        helperActor = Some(system.actorOf(Props(new Actor() {
          override def receive: Receive = Actor.emptyBehavior
        }), "DispatcherHelperActor"))

        // read the virtual time step config and create TimerActor
        var timeStep: FiniteDuration =
          try {
            FiniteDuration(ConfigFactory.load(DispatcherUtils.debuggerConfigFile).getDuration("pct-dispatcher.timestep", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
          } catch {
            case e: Exception =>
              println(e + "\n No valid timestep duration provided in configuration file. Using default timestep 1 MILLISECONDS")
              FiniteDuration(1, TimeUnit.MILLISECONDS)
          }

        val timer = system.actorOf(Props(new TimerActor(timeStep)), "Timer")
        timerActor = Some(timer)
        timer ! AdvanceTime
        ioProvider.setUp(system) // if ioProvider is network, start the server etc.
      case _ => // do nth
    }
  }

  def sendToDispatcher(msg: Any): Unit = helperActor match {
    case Some(actor) => actor ! msg
    case None => printDispatcherLog(Events.LOG_WARNING, "Cannot send to the dispatcher, no helper actor is created")
  }

  def printDispatcherLog(logType: Int, text: String): Unit = logType match {
    case Events.LOG_WARNING | Events.LOG_ERROR => CmdLineUtils.printlnForWELogging(text)
    case _ => CmdLineUtils.printlnForLogging(text)
  }

  //def getActorMessages: List[(Cell, List[Envelope])] = actorMessagesMap.toList.sortBy(_._1.self.toString())

  def getActorIdByIndex(index: Int): String = actorMessagesMap.getActorIdByIndex(index)

  def getActorNameByIndex(index: Int): String = actorMessagesMap.getActorNameByIndex(index)

  def listOfActorsByCell: List[(Cell, List[Envelope])] = actorMessagesMap.toListWithActorCell

  def listOfActorsByRef: List[(ActorRef, List[Envelope])] = actorMessagesMap.toListWithActorRef

  def listOfActorsByName: List[(String, List[Envelope])] = actorMessagesMap.toListWithActorPath

  /**
    * Used to get the internal state of an actor
    * The user actors must be implemented to accept this message and to respond this message with its internal state
    */
  case object GetInternalActorState

}


/**
  * An extension of Dispatcher with logging facilities
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
  private def handleInitiate(): ActionResponse = {
    println(Thread.currentThread().getName)
    ActionResponse(ProgramEvents.consumeEvents)
  }

  private def handleTerminate: Any = actorSystem match {
    case Some(system) => system.terminate
    case None => printDispatcherLog(Events.LOG_WARNING, "Cannot terminate")
  }

  def handleDispatchToActor(actor: Cell): ActionResponse = {
    actorMessagesMap.removeHeadMessage(actor) match {
      case Some(envelope) =>
        ProgramEvents.addEvent(MessageReceived(actor.self.path.toString, envelope.sender.path.toString, envelope.message.toString))
        // handle the actor message synchronously
        val receiver = actor.asInstanceOf[ActorCell]
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, envelope)
        processMailbox(mbox)
        ActionResponse(ProgramEvents.consumeEvents)

      case None =>
        printDispatcherLog(Events.LOG_WARNING, "Dispatch from an actor with no messages.")
        ActionResponse(List(Log(Events.LOG_WARNING, "Dispatch from an actor with no messages.")))
    }
  }

  def handleDropActorMsg(actor: Cell): ActionResponse = {
    actorMessagesMap.removeHeadMessage(actor) match {
      case Some(envelope) =>
        println("Removed head message from: " + actor.self.path + " msg: " + envelope)
        ProgramEvents.addEvent(MessageDropped(actor.self.path.toString, envelope.sender.path.toString, envelope.message.toString))
        ActionResponse(ProgramEvents.consumeEvents)

      case None =>
        printDispatcherLog(Events.LOG_WARNING, "Msg drop from an actor with no messages.")
        ActionResponse(List(Log(Events.LOG_WARNING, "Msg drop from an actor with no messages.")))
    }
  }

  /**
    * Called when an actor has crashed
    */
  def sendErrResponse(event: Log, actor: ActorRef): Unit = {
    // run on the dispatcher thread async
    executorService execute new Runnable() {
      override def run(): Unit = {
        ProgramEvents.addEvent(event)
        actorMessagesMap.getActor(actor.path.toString) match {
          case Some(a) => ioProvider.putResponse(ActionResponse(ProgramEvents.consumeEvents))
          case None => ioProvider.putResponse(ActionResponse(ProgramEvents.consumeEvents))
        }
      }
    }
  }

  private def processMailbox(mbox: Mailbox): Unit = {
    if(mbox.hasMessages) { // DebuggerDispatcher runs this method after enqueuing a message
      ReflectionUtils.callPrivateMethod(mbox, "processAllSystemMessages")()
      ReflectionUtils.callPrivateMethod(mbox, "processMailbox")(1, 0L)
    } else {
      CmdLineUtils.printlnForWELogging("Mailbox does not have any messages: " + mbox.messageQueue.numberOfMessages + "   " + mbox.messageQueue.toString)
    }
  }

  private def checkAndWaitForActorBehavior(actor: ActorCell): Unit = {
    if(ReflectionUtils.readPrivateVal(actor, "behaviorStack").asInstanceOf[List[Actor.Receive]] == List.empty) {
      CmdLineUtils.printlnForLogging("Actor behavior is not set. Cannot process mailbox. Trying again..")
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
    *
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    //println("In dispatch : " + invocation + " " + Thread.currentThread().getName)

    invocation match {
      // Handle Dispatcher messages
      case Envelope(msg, _) if msg.isInstanceOf[DispatcherMsg] =>

        msg match {
        case DispatchToActor(actor) =>
          runOnExecutor(toRunnable(() => {
            ioProvider.putResponse(handleDispatchToActor(actor))
          }))
          return
        case DropActorMsg(actor) =>
          runOnExecutor(toRunnable(() => {
            ioProvider.putResponse(handleDropActorMsg(actor))
          }))
          return
        case InitDispatcher =>
          runOnExecutor(toRunnable(() => {
            ioProvider.putResponse(handleInitiate())
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
        CmdLineUtils.printlnForLogging("Log msg is delivered. Running synchronously. " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        //registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        // Instead of posting the msg handler runnable, synchronously run it (the msg handler just does logging)
        processMailbox(mbox)
        return

      // Do not intercept the messages sent to the system actors
      case _ if DispatcherUtils.isSystemActor(receiver.self) =>
        //CmdLineUtils.printlnForLogging("Not intercepted msg to system actor: " + receiver.self + " " + invocation)
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
        //CmdLineUtils.printlnForLogging("Not intercepted internal msg To: " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ =>
        // if the message is sent by the Ask Pattern:
        if (invocation.sender.isInstanceOf[PromiseActorRef]) {
          CmdLineUtils.printlnForLogging("-- Message by AskPattern. Sending for execution. " + receiver.self + " " + invocation)
          checkAndWaitForActorBehavior(receiver)
          val mbox = receiver.mailbox
          mbox.enqueue(receiver.self, invocation)
          // registerForExecution posts the msg processing runnable to the thread pool executor
          // (It gets blocked since the only thread in the thread pool is possibly waiting on a future)
          // In case of ?, the msg is synchronously executed in the dispatcher thread

          // what if the handler of this thread waits as well?
          // Sln: Run each on a fresh thread..
          val t = new Thread(toRunnable(() => processMailbox(mbox)))
          t.start()
          t.join(10000)
          return
        }

      // Go with the default execution, intercept and record the message
    }

    CmdLineUtils.printlnForLogging("Intercepting msg to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)

    // Add the intercepted message into the list of output events
    ProgramEvents.addEvent(MessageSent(receiver.self.path.toString, invocation.sender.path.toString(), invocation.message.toString))

    // Add the intercepted message into intercepted messages map
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
    CmdLineUtils.printlnForLogging("Delivered system msg: " + invocation + "   Actor: " + receiver.self)

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
      case Create(failure: Option[ActorInitializationException]) => CmdLineUtils.printlnForLogging("Handling system msg: Create by failure: " + failure)
      case Recreate(cause: Throwable) => CmdLineUtils.printlnForLogging("Handling system msg: Recreate by cause: " + cause)
      case Suspend() => CmdLineUtils.printlnForLogging("Handling system msg: Suspend")
      case Resume(causedByFailure: Throwable) => CmdLineUtils.printlnForLogging("Handling system msg: Resume by failure: " + causedByFailure)
      case Terminate() =>
        CmdLineUtils.printlnForLogging("Handling system msg terminates: " + receiver.self)
        // add to the event list only if it is not a system actor
        if (!DispatcherUtils.isSystemActor(receiver.self)) {
          ProgramEvents.addEvent(ActorDestroyed(receiver.self.path.toString))
          actorMessagesMap.removeActor(receiver)
        }
      case Supervise(child: ActorRef, async: Boolean) => CmdLineUtils.printlnForLogging("Handling system msg: Supervise. Child: " + child)
      case Watch(watchee: InternalActorRef, watcher: InternalActorRef) => CmdLineUtils.printlnForLogging("Handling system msg: Watch. Watchee: " + watchee + " Watcher: " + watcher)
      case Unwatch(watchee: ActorRef, watcher: ActorRef) => CmdLineUtils.printlnForLogging("Handling system msg: Unwatch. Watchee: " + watchee + " Watcher: " + watcher)
      case NoMessage => CmdLineUtils.printlnForLogging("Handling system msg: NoMessage")
      case _ => // do not track the other system messages for now
    }
  })

  /**
    * Overriden to add the actors with the created mailbox into the ActorMap
    */
  override def createMailbox(actor: akka.actor.Cell, mailboxType: MailboxType): Mailbox = {
    CmdLineUtils.printlnForLogging("Created mailbox for: " + actor.self + " in thread: " + Thread.currentThread().getName)

    // add to the event list only if it is not a system actor
    if (!DispatcherUtils.isSystemActor(actor.self) /*&& !actor.self.toString().startsWith("Actor[akka://" + systemName + "/user/" + dispatcherInitActorName)*/ ) {
      ProgramEvents.addEvent(ActorCreated(actor.self.path.toString))
      actorMessagesMap.addActor(actor)
    }

    new Mailbox(mailboxType.create(Some(actor.self), Some(actor.system))) with DefaultSystemMessageQueue
  }

  /**
    * Overriden to output the recorded events
    */
  override def shutdown: Unit = {
    CmdLineUtils.printlnForLogging("Shutting down.. ")

    FileUtils.printToFile("allEvents") { p =>
      ProgramEvents.getAllEvents.foreach(p.println)
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
