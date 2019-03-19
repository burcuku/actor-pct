package akka.dispatch

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import collection.JavaConverters._

import scala.concurrent.duration.FiniteDuration


object DispatcherOptions {

  val configFile: String = "dispatcher.conf"

  private val useTimerPath = "testing-dispatcher.useVirtualTimer"
  private val timeStepPath = "testing-dispatcher.timestep"
  private val logLevelPath = "testing-dispatcher.logLevel"
  private val schedulerPath = "testing-dispatcher.scheduler"
  private val willTerminatePath = "testing-dispatcher.willTerminate"
  private val maxNumTimeStepsPath = "testing-dispatcher.maxNumTimeSteps"
  private val networkDelayPath = "testing-dispatcher.networkDelay"
  private val noInterceptMsgsPath = "testing-dispatcher.noInterceptMsgs"
  val defaultUiChoice = "CmdLine"

  val config: Config = ConfigFactory.load(configFile)

  val logLevel: Int = if(config.hasPath(logLevelPath)) config.getInt(logLevelPath) else 1 //CmdLineUtils.LOG_INFO

  val useTimer: Boolean = if(config.hasPath(useTimerPath)) config.getBoolean(useTimerPath) else false

  val willTerminate: Boolean = if(config.hasPath(willTerminatePath)) config.getBoolean(willTerminatePath) else false

  val maxNumTimeSteps: Int = if(config.hasPath(maxNumTimeStepsPath)) config.getInt(maxNumTimeStepsPath) else 10

  val networkDelay: Int = if(config.hasPath(networkDelayPath)) config.getInt(networkDelayPath) else 0

  val noInterceptMsgs = if(config.hasPath(noInterceptMsgsPath)) config.getStringList(noInterceptMsgsPath).asScala.toList else List()

  lazy val timeStep: FiniteDuration =
    if(config.hasPath(timeStepPath)) FiniteDuration(config.getDuration(timeStepPath, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    else {
      println("No valid timestep duration provided in configuration file. Using default timestep 1 MILLISECONDS")
      FiniteDuration(1, TimeUnit.MILLISECONDS)
    }

  // CMDLINE, RANDOM, PCTCP, taPCTCP, POS, dPOS, RAPOS
  val scheduler: String =
    if(config.hasPath(schedulerPath)) config.getString(schedulerPath)
    else {
      println("Input choice is not provided in the configuration file. Using " + defaultUiChoice + " by default")
      defaultUiChoice
    }


  // options related to scheduler:

  private val bugDepthPath = "testing-dispatcher.bugDepth"
  private val maxMessagesPath = "testing-dispatcher.maxMessages"
  private val maxRacyMessagesPath = "testing-dispatcher.maxRacyMessages"
  private val algorithmPath = "testing-dispatcher.algorithm"

  private val randomSeedPath = "testing-dispatcher.seed"

  private val defaultBugDepth = 1
  private val defaultMaxMessages = 10
  private val defaultMaxRacyMessages = 0
  private val defaultAlgorithm = "AG"

  val randomSeed: Long = if(config.hasPath(randomSeedPath)) ConfigFactory.load(configFile).getLong(randomSeedPath) else System.currentTimeMillis()
  val bugDepth: Int = if(config.hasPath(bugDepthPath)) ConfigFactory.load(configFile).getInt(bugDepthPath) else defaultBugDepth
  val maxMessages: Int = if(config.hasPath(maxMessagesPath)) ConfigFactory.load(configFile).getInt(maxMessagesPath) else defaultMaxMessages
  val maxRacyMessages: Int = if(config.hasPath(maxRacyMessagesPath)) ConfigFactory.load(configFile).getInt(maxRacyMessagesPath) else defaultMaxMessages
  val algorithm: String = if(config.hasPath(algorithmPath)) {
    val option = ConfigFactory.load(configFile).getString(algorithmPath).toUpperCase
    if (Set("AG", "BM").contains(option)) option else defaultAlgorithm
  } else defaultAlgorithm

}
