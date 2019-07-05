import akka.actor.{Actor, ActorSystem, Props}
import server.{DispatcherServer, ExplorerServer, ServerConfig}


object Main extends App {

  val config = new ServerConfig("remote.conf")
  val system = ActorSystem("sys")

  val explorerServer = system.actorOf(ExplorerServer.props(config), "TcpServerForExplorer")
  val dispatcherServer = system.actorOf(DispatcherServer.props(config, explorerServer), "TcpServerForDispatcher")

  // Note: Make sure that explorer process is connected before the dispatcher process

  println("Using the configuration file: " + "remote.conf")
}
