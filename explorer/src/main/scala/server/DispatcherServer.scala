package server

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress
import spray.json._

import scala.collection.mutable

/**
  * The explorer connects to this server and sends exploration requests
  * (The requests are accepted in QueryRequest json format)
  * The TcpServerAcceptingResponses sends Responses to this Server
  */
class DispatcherServer(serverConfig: ServerConfig, explorerServer: ActorRef) extends Actor {

  def receive: Receive = ???
}

object DispatcherServer {
  def props(serverConfig: ServerConfig, explorerServer: ActorRef): Props = Props(new DispatcherServer(serverConfig, explorerServer))
}

