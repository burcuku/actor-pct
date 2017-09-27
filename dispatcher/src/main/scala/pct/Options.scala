package pct

import akka.dispatch.DispatcherOptions.{config, uiChoicePath}
import com.typesafe.config.ConfigFactory

object Options {

  val configFile: String = "dispatcher.conf"

  private val bugDepthPath = "pct-dispatcher.bugDepth"
  private val maxMessagesPath = "pct-dispatcher.maxMessages"
  private val algorithmPath = "pct-dispatcher.algorithm"

  private val defaultBugDepth = 1
  private val defaultMaxMessages = 10
  private val defaultAlgorithm = "AG"

  val bugDepth: Int = if(config.hasPath(bugDepthPath)) ConfigFactory.load(configFile).getInt(bugDepthPath) else defaultBugDepth
  val maxMessages: Int = if(config.hasPath(maxMessagesPath)) ConfigFactory.load(configFile).getInt(maxMessagesPath) else defaultMaxMessages
  val algorithm: String = if(config.hasPath(algorithmPath)) {
      val option = ConfigFactory.load(configFile).getString(algorithmPath).toUpperCase
      if (Set("AG", "BM").contains(option)) option else defaultAlgorithm
    } else defaultAlgorithm

  def print(): Unit = {
    println("PCT OPTIONS:")
    println("Bug depth: " + bugDepth)
    println("Max number of messages: " + maxMessages)
    println("Chain partitioning algorithm: " + algorithm)
  }
}
