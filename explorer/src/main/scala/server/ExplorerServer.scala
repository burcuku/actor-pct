package server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import spray.json._

/**
  * The dispatcher process connects to this server:
  * The dispatcher process is forwarded the received user requests
  * The responses received from the dispatcher are passed to the TcpServerForExplorer
  * (The response (i.e., configuration) from the dispatcher are accepted in QueryResponse json format)
  */
class ExplorerServer(serverConfig: ServerConfig) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", serverConfig.explorerPort))

  def receive: Receive = {
    case _ => println("Not implemented handler")
  }

}

object ExplorerServer {
  def props(serverConfig: ServerConfig): Props = Props(new ExplorerServer(serverConfig))
}
