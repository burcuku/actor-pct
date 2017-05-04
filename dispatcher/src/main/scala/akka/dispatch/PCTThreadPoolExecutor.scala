package akka.dispatch

import java.util.concurrent._

import akka.dispatch.ThreadPoolConfig.QueueFactory

import scala.concurrent.duration.Duration

class PCTThreadPoolExecutor(corePoolSize: Int, maxPoolSize: Int, threadTimeout: Duration,
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

final case class PCTThreadPoolConfig(allowCorePoolTimeout: Boolean = ThreadPoolConfig.defaultAllowCoreThreadTimeout,
                                  corePoolSize: Int = 2, //ThreadPoolConfig.defaultCorePoolSize,
                                  maxPoolSize: Int = 8, //ThreadPoolConfig.defaultMaxPoolSize,
                                  threadTimeout: Duration = ThreadPoolConfig.defaultTimeout,
                                  queueFactory: ThreadPoolConfig.QueueFactory = ThreadPoolConfig.linkedBlockingQueue(),
                                  rejectionPolicy: RejectedExecutionHandler = ThreadPoolConfig.defaultRejectionPolicy)
  extends ExecutorServiceFactoryProvider {
  class ThreadPoolExecutorServiceFactory(val threadFactory: ThreadFactory) extends ExecutorServiceFactory {
    def createExecutorService: ExecutorService = {
      val service = new PCTThreadPoolExecutor(
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

