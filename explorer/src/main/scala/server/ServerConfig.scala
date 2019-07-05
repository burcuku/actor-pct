package server

import com.typesafe.config.ConfigFactory

import scala.util.Try

class ServerConfig(configFileName: String) {

  private[this] val config = ConfigFactory.load(configFileName)

  val dispatcherPort: Int = Try(config.getInt("explorer-server.dispatcherPort")).getOrElse(2772) //todo multiple!

  val explorerPort: Int = Try(config.getInt("explorer-server.explorerPort")).getOrElse(5555)
}
