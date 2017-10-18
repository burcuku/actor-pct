package akka.dispatch

import java.util.concurrent._

import akka.actor.ActorCell
import akka.dispatch.ThreadPoolConfig.QueueFactory
import akka.dispatch.util.{CmdLineUtils, FileUtils}
import akka.event.Logging.{Debug, Error, Info, Warning}
import akka.pattern.PromiseActorRef
import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random


object RandomDelayDispatcher {
  val random = new Random(RandomDelayOptions.randomSeed)
  var maxDelay: Int = RandomDelayOptions.MIN_POSSIBLE_DELAY // no delay

  // to be set programmatically, based on app parameters
  def setSeed(randomSeed: Long): Unit = random.setSeed(randomSeed)
  def setMaxDelay(delay: Int): Unit =
    if(delay > 0 && delay <= RandomDelayOptions.MAX_POSSIBLE_DELAY) maxDelay = delay
    else println("The delay parameter is not between " + RandomDelayOptions.MIN_POSSIBLE_DELAY + " and " + RandomDelayOptions.MAX_POSSIBLE_DELAY)
}

/**
  * An extension of Dispatcher with randomly delaying messages
  */
final class RandomDelayDispatcher(_configurator: MessageDispatcherConfigurator,
                          _id: String,
                          _shutdownTimeout: FiniteDuration)
  extends Dispatcher(
    _configurator,
    _id,
    Int.MaxValue,
    Duration.Zero,
    RandomDelayThreadPoolConfig(),
    _shutdownTimeout) {

  import RandomDelayDispatcher._

  var numScheduledMsgs = 0
  var numDelayedMsgs = 0
  var totalDelayAmount = 0

  /**
    * Overriden to randomly delay messages
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = invocation match {

    case Envelope(Error(_, _, _, _), _)
         | Envelope(Warning(_, _, _), _)
         | Envelope(Info(_, _, _), _)
         | Envelope(Debug(_, _, _), _) =>
      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, invocation)
      registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)

    // do not delay or count ask pattern messages
    case _ =>
      if(!invocation.sender.isInstanceOf[PromiseActorRef]) {
        if(random.nextBoolean()) {
          val delay = random.nextInt(maxDelay)
          println( "Delayed: " + delay + "msec Msg to: " + receiver + " " + invocation + " In: " + Thread.currentThread().getName)
          Thread.sleep(delay)
          numDelayedMsgs = numDelayedMsgs + 1
          totalDelayAmount = totalDelayAmount + delay
        } else {
          println( "Not Delayed Msg to: " + receiver + " " + invocation + " In: " + Thread.currentThread().getName)
        }
        numScheduledMsgs = numScheduledMsgs + 1
      }

      val mbox = receiver.mailbox
      mbox.enqueue(receiver.self, invocation)
      registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
  }

  override def shutdown: Unit = {
    CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Shutting down.. ")
    logInfo()
    super.shutdown
  }

  def logInfo(): Unit = {
    FileUtils.printToFile("stats") { p =>
      p.println("Random Delayer Scheduler Stats: \n")
      p.println("RandomSeed: " + RandomDelayOptions.randomSeed)
      p.println("MaxDelay: " + RandomDelayOptions.maxDelay)
      p.println()
      p.println("NumScheduledMsgs: " + numScheduledMsgs)
      p.println("NumDelayedMsgs: " + numDelayedMsgs)
      p.println("AverageDelayAmount in msec: " + (totalDelayAmount.toDouble / numDelayedMsgs.toDouble))
    }
  }
}

class RandomDelayDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new RandomDelayDispatcher(
    this,
    config.getString("id"),
    Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}

final case class RandomDelayThreadPoolConfig(allowCorePoolTimeout: Boolean = ThreadPoolConfig.defaultAllowCoreThreadTimeout,
                                     corePoolSize: Int = ThreadPoolConfig.defaultCorePoolSize,
                                     maxPoolSize: Int = ThreadPoolConfig.defaultMaxPoolSize,
                                     threadTimeout: Duration = ThreadPoolConfig.defaultTimeout,
                                     queueFactory: ThreadPoolConfig.QueueFactory = ThreadPoolConfig.linkedBlockingQueue(),
                                     rejectionPolicy: RejectedExecutionHandler = ThreadPoolConfig.defaultRejectionPolicy)
  extends ExecutorServiceFactoryProvider {
  class ThreadPoolExecutorServiceFactory(val threadFactory: ThreadFactory) extends ExecutorServiceFactory {
    def createExecutorService: ExecutorService = {
      val service = new RandomDelayThreadPoolExecutor(
        corePoolSize,
        maxPoolSize,
        threadTimeout,
        threadFactory,
        queueFactory,
        rejectionPolicy) with LoadMetrics {
        def atFullThrottle(): Boolean = this.getActiveCount >= this.getPoolSize
      }
      service.allowCoreThreadTimeOut(allowCorePoolTimeout)
      service
    }
  }

  def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val tf = threadFactory match {
      case m: MonitorableThreadFactory ⇒
        // add the dispatcher id to the thread names
        m.withName(m.name + "-" + id)
      case other ⇒ other
    }
    new ThreadPoolExecutorServiceFactory(tf)
  }
}

class RandomDelayThreadPoolExecutor(corePoolSize: Int, maxPoolSize: Int, threadTimeout: Duration,
                            threadFactory: ThreadFactory, queueFactory: QueueFactory, rejectionPolicy: RejectedExecutionHandler)
  extends ThreadPoolExecutor(corePoolSize, maxPoolSize, threadTimeout.length, threadTimeout.unit, queueFactory(), threadFactory, rejectionPolicy) {

  /**
    * Currently not used before/after execute methods, might be needed for logging purposes
    */
  override def beforeExecute(t: Thread, r: Runnable): Unit = {
  }

  override def afterExecute(r: Runnable, t: Throwable): Unit = {
  }
}