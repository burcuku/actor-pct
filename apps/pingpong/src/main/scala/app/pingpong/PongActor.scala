package app.pingpong

import akka.actor.{Actor, ActorLogging, Props}

class PongActor extends Actor with ActorLogging{
  import PongActor._

  def receive = {
  	case PingActor.PingMessage(text) =>
  	  log.info("In PongActor - received message: {} in {}", text, Thread.currentThread().getName)
      val tokens = text.split(" ")
      if (tokens.size == 2 && tokens(0).equals("ping"))
        sender() ! PongMessage("pong " + tokens(1) )
  }
}

object PongActor {
  val props = Props[PongActor]
  case class PongMessage(text: String)
}
