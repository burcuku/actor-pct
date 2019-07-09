package scheduler.network

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.TestingDispatcher.AddedEvents
import akka.dispatch.{MessageReceived, MessageSent, RequestHandler, SchedulingStrategy}
import akka.dispatch.util.CmdLineUtils
import akka.io.Tcp.Connected
import akka.util.ByteString
import explorer.protocol
import explorer.protocol.Requests.RequestJsonFormat
import explorer.protocol.{CommandResponse, Event, Response}
import explorer.protocol.Responses.ResponseJsonFormat
import scheduler.SchedulerOptions
import spray.json._

class NetworkStrategy(options: SchedulerOptions) extends  SchedulingStrategy {
  var clientActor: Option[ActorRef] = None
  var invoker: ActorRef = Actor.noSender

  override def setUp(system: ActorSystem): Unit = {
    /**
      * Communicates with the server via TCPClient
      * Receives QueryResponse from the server to be handled
      * Sends QueryResponse of the running program
      */
    invoker = system.actorOf(Props(new Actor() {
      override def receive: Receive = {
        case Connected(remoteAddr, localAddr) => {
          println("Client received: " + remoteAddr + "  " + localAddr)
        }
        // receives the QueryRequest sent by the Server
        case s: ByteString => {
          val request = RequestJsonFormat.read(s.utf8String.parseJson)
          CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Received user request: " + request)
          RequestHandler.handleRequest(request)
        }
        // when receives a QueryResponse (from the dispatcher), sends it to the server
        case AddedEvents(internalEvents, predecessors) => {
          CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Sending program response: " + internalEvents + " " + predecessors)

          //adapts
          val progEvents = internalEvents.filter(e => e._2.isInstanceOf[MessageSent])
            .map(e => Event(e._1, protocol.MessageSent(
              e._2.asInstanceOf[MessageSent].receiver.self.toString(),
              e._2.asInstanceOf[MessageSent].msg.sender.toString(),
              e._2.asInstanceOf[MessageSent].msg.message.toString)))

          clientActor match {
            case Some(actor) => actor ! ByteString(ResponseJsonFormat.write(CommandResponse(progEvents.toList, predecessors)).prettyPrint)
            case None => println(Console.RED + "Client actor has not created yet" + Console.RESET)
          }
        }
      }
    }), "DispatcherTCPInvoker")
    // todo parametrize
    clientActor = Some(system.actorOf(TcpClient.props(new InetSocketAddress("localhost", 5555), invoker), "DispatcherTCPClientActor"))
  }

  override def putResponse(response: Response): Unit = {
    invoker ! response
  }
}

