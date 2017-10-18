package akka.dispatch

import com.typesafe.config.{Config, ConfigFactory}

object RandomDelayOptions {

  val configFile: String = "delayer-dispatcher.conf"
  val config: Config = ConfigFactory.load(configFile)
  val MAX_POSSIBLE_DELAY = 300 // msec
  val MIN_POSSIBLE_DELAY = 1 //  no delay

  private val randomSeedPath = "delayer-dispatcher.randomSeed"
  private val maxDelayPath = "delayer-dispatcher.maxDelay"

  val randomSeed: Long = if(config.hasPath(randomSeedPath)) ConfigFactory.load(configFile).getLong(randomSeedPath) else System.currentTimeMillis()
  val maxDelay: Int = if(config.hasPath(maxDelayPath)) ConfigFactory.load(configFile).getInt(maxDelayPath) else 0

  def print(): Unit = {
    println("Random Delayer Dispatcher Options:")
    println("Random seed: " + randomSeed)
    println("Max delay: " + maxDelay)
  }
}