import akka.actor.{Actor, ActorSystem, Props}
import server.{ExplorerServer, ServerConfig}


object Main extends App {

  val config = new ServerConfig("remote.conf")
  val system = ActorSystem("sys")

  val explorerServer = system.actorOf(ExplorerServer.props(config), "TcpServerForExplorer")

  println("Using the configuration file: " + "remote.conf")
}
