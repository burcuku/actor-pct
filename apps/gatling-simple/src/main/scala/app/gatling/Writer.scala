package app.gatling

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable.ListBuffer

class Writer extends Actor with ActorLogging {
  import Writer._

  var results: ListBuffer[String] = ListBuffer()

  def receive = {
    case Write(result: String) =>
      results.append(result)
    case Flush =>
      writeToExternal(results.toList)
      results = null
      sender ! Flushed
  }

  def writeToExternal(result: List[String]) = {}
}

object Writer {
  val props: Props = Props(new Writer)
  case class Write(msg: String)
  case object Flush
  case object Flushed
}