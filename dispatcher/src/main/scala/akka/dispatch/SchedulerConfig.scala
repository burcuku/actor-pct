package akka.dispatch

import akka.dispatch.DispatcherOptions.config
import com.typesafe.config.ConfigFactory

object SchedulerConfig {
/*
  val configFile: String = "dispatcher.conf"

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
*/
}