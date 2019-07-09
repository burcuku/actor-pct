package server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp.Write
import akka.io.{IO, Tcp}
import akka.util.ByteString
import explorer.protocol.Requests.RequestJsonFormat
import explorer.protocol.Responses.ResponseJsonFormat
import spray.json._
import explorer.protocol.{InitRequest, NewEvents, Request, Response}

/**
  * The dispatcher process connects to this server to:
  *   - receive CommandRequest and
  *   - send CommandResponse
  */
class ExplorerServer(serverConfig: ServerConfig) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", serverConfig.explorerPort))

  //todo initiate explorer with a parameter of this
  private val explorer: ActorRef = context.actorOf(ExplorerActor.props(self), "explorerActor")

  /**
    * Receives QueryResponse (by the connected dispatcher process) and sends it to the explorer
    */
  val responseHandler: ActorRef = context.actorOf(CommandResponseHandlerActor.props(explorer), "responseHandler")

  /**
    * Receives QueryRequest (from the explorer) and sends this user request to the connected dispatcher process
    */
  val requestHandler: ActorRef = context.actorOf(CommandRequestHandlerActor.props, "requestHandler")

  def receive: Receive = {
    case b @ Bound(localAddress) =>
      println(Console.BLUE + "Tcp server for debugger process bound" + Console.RESET)

    case CommandFailed(_: Bind) => context stop self

    // when a client (dispatcher process) connects, save this connection
    case c @ Connected(remote, local) =>
      val connection = sender()
      connection ! Register(responseHandler)
      requestHandler ! AddClient(connection)

    // receives from the Explorer
    case request: Request =>
      println(Console.BLUE + "Server received request: " + request + Console.RESET)
      requestHandler ! request

    // receives from the Dispatcher
    case response: Response =>
      println(Console.BLUE + "Server received response: " + response + Console.RESET)
      responseHandler ! response
  }
}

object ExplorerServer {
  def props(serverConfig: ServerConfig): Props = Props(new ExplorerServer(serverConfig))
}

/**
  * Forwards the explorer's request to the dispatcher
  */
class CommandRequestHandlerActor extends Actor {
  // The debugger process (to whom the user request will be sent)
  private var dispatcher: ActorRef = Actor.noSender

  def receive: Receive = {
    case AddClient(c) =>
      println(Console.BLUE + "Dispatcher process client set to: " + c + Console.RESET)
      dispatcher = c
      dispatcher ! Write(ByteString(RequestJsonFormat.write(InitRequest).prettyPrint))

    case commandRequest: Request =>
      // todo send request to dispatcher (via network)
      if(!dispatcher.equals(Actor.noSender)) {
        println(Console.BLUE + "Request to be sent to the debugger: " + RequestJsonFormat.write(commandRequest).prettyPrint + Console.RESET)
        dispatcher ! Write(ByteString(RequestJsonFormat.write(commandRequest).prettyPrint))
      } else {
        println(Console.RED_B + "Received request before the debugger process connection!")
        println("Request cannot be sent!" + Console.RESET)
      }

    case _ =>
  }
}

object CommandRequestHandlerActor {
  def props = Props(new CommandRequestHandlerActor())
}

/**
  * Receives program output from the dispatcher process
  * Forwards this response to the explorer
  */
class CommandResponseHandlerActor(explorer: ActorRef) extends Actor {
  import Tcp._

  def receive: Receive = {
    case Received(data) =>
      // todo decode the network data and send to explorer
      try {
        // The received data must be a QueryResponse
        val response = ResponseJsonFormat.read(data.utf8String.parseJson)
        println(Console.BLUE + "Response to be sent to explorer: " + response + Console.RESET)
        val pairs = response.events.map(e => (e.id, e.programEvent))
        explorer ! NewEvents(pairs, response.predecessors)
      } catch {
        case e: Exception => println(Console.BLUE + "Server could not parse the response: " + data.utf8String + Console.RESET)
      }

    case PeerClosed => println("Debugger server - Peer closed")
    case _ =>
  }
}

object CommandResponseHandlerActor {
  def props(explorer: ActorRef) = Props(new CommandResponseHandlerActor(explorer))
}

case class AddClient(client: ActorRef)
