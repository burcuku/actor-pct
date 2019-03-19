package app.pingpong

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class PingActor(pongActor: ActorRef, maxCount: Int, willTerminateSystem: Boolean) extends Actor
  with ActorLogging {
  import PingActor._
  
  var counter = 0

  def receive: PartialFunction[Any, Unit] = {
  	case Initialize => 
	    log.info("In PingActor - starting ping-pong " + Thread.currentThread().getName)
  	  pongActor ! PingMessage("ping 0")
      //self ! Dummy
  	case PongActor.PongMessage(text) =>
  	  log.info("In PingActor - received message: " + text + " in " + Thread.currentThread().getName)
  	  counter += 1
      if (counter <= maxCount) {
        sender() ! PingMessage("ping " + counter)
      }
      else if (willTerminateSystem) context.system.terminate()

    case Dummy => println("Got dummy message")
  }
}

object PingActor {
  val props: Props = Props[PingActor]
  case object Initialize
  case class PingMessage(text: String)
  case object Dummy
}