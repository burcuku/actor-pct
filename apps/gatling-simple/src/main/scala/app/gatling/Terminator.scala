package app.gatling

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import app.gatling.Terminator.ActionDone
import app.gatling.Writer.Flush


class Terminator(actionNum: Int, writer: ActorRef) extends Actor with ActorLogging{
  var curActions = actionNum

  def receive = {
  	case ActionDone =>
      curActions = curActions - 1
  	  log.info("Terminator received ActionDone message")
      if (curActions == 0) writer ! Flush
  }
}

object Terminator {
  def props(actionNum: Int, writer: ActorRef) = Props(new Terminator(actionNum, writer: ActorRef))
  case object ActionDone
}


