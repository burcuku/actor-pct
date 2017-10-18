package akka.dispatch

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.FiniteDuration


object DispatcherOptions {

  val debuggerConfigFile: String = "dispatcher.conf"

  private val useTimerPath = "pct-dispatcher.useVirtualTimer"
  private val timeStepPath = "pct-dispatcher.timestep"
  private val logLevelPath = "pct-dispatcher.logLevel"
  private val uiChoicePath = "pct-dispatcher.inputChoice"
  private val willTerminatePath = "pct-dispatcher.willTerminate"
  private val maxNumTimeStepsPath = "pct-dispatcher.maxNumTimeSteps"
  private val networkDelayPath = "pct-dispatcher.networkDelay"
  val defaultUiChoice = "CmdLine"

  val config: Config = ConfigFactory.load(debuggerConfigFile)

  val logLevel: Int = if(config.hasPath(logLevelPath)) config.getInt(logLevelPath) else 1 //CmdLineUtils.LOG_INFO

  val useTimer: Boolean = if(config.hasPath(useTimerPath)) config.getBoolean(useTimerPath) else false

  val willTerminate: Boolean = if(config.hasPath(willTerminatePath)) config.getBoolean(willTerminatePath) else false

  val maxNumTimeSteps: Int = if(config.hasPath(maxNumTimeStepsPath)) config.getInt(maxNumTimeStepsPath) else 10

  val networkDelay: Int = if(config.hasPath(networkDelayPath)) config.getInt(networkDelayPath) else 0

  lazy val timeStep: FiniteDuration =
    if(config.hasPath(timeStepPath)) FiniteDuration(config.getDuration(timeStepPath, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
    else {
      println("No valid timestep duration provided in configuration file. Using default timestep 1 MILLISECONDS")
      FiniteDuration(1, TimeUnit.MILLISECONDS)
    }

  val uiChoice: String =
    if(config.hasPath(uiChoicePath)) config.getString(uiChoicePath)
    else {
      println("Input choice is not provided in the configuration file. Using " + defaultUiChoice + " by default")
      defaultUiChoice
    }
}
