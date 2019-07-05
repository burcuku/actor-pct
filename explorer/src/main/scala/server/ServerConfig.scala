package server

import com.typesafe.config.ConfigFactory

import scala.util.Try

class ServerConfig(configFileName: String) {

  private[this] val config = ConfigFactory.load(configFileName)

  val explorerPort: Int = Try(config.getInt("explorer-server.explorerPort")).getOrElse(5555)
}
