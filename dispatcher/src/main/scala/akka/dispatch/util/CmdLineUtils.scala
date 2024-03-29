package akka.dispatch.util

import akka.dispatch.DispatcherOptions

import scala.collection.immutable.List

object CmdLineUtils {

  /** Parser for the user input to CmdLineIO **/
  def parseInput(allowedStrs: List[String]): (String, Int) = {
    val line = Console.in.readLine().split(" ")

    if (line.isEmpty || !allowedStrs.contains(line(0))) {
      println("Wrong input, try again. ")
      return parseInput(allowedStrs)
    }

    if (line(0).equalsIgnoreCase("start") || line(0).equalsIgnoreCase("quit")) (line(0), 0)
    else if (line.size != 2 || !line(1).charAt(0).isDigit) {
      println("Wrong input, try again. ")
      parseInput(allowedStrs)
    } else {
      (line(0), line(1).toInt)
    }
  }

  private val logLevel: Int = DispatcherOptions.logLevel

  def printLog(logType: Int, s: String): Unit = logType match {
    case LOG_DEBUG if logLevel <= LOG_DEBUG => println(s)
    case LOG_INFO if logLevel <= LOG_INFO => println(s)
    case LOG_WARNING if logLevel <= LOG_WARNING => println(Console.CYAN + s + Console.RESET)
    case LOG_ERROR if logLevel <= LOG_ERROR => println(Console.RED + s + Console.RESET)
    case _ => // do nth
  }

  def printlnForUiInput(s:String): Unit = println(Console.BLUE + s + Console.RESET)

  /**
    * Fancy printing of a map
    */
  def printMap[K, V](map: Map[K, V]): Unit = map.foreach(a => println(a._1 + "  ==>  " + a._2))

  /**
    * Fancy printing of a map converted to a list
    * (For the case of Actor messages, Key <- ActorRef and ListElement <- Envelope)
    */
  def printListOfMap[Key, ListElement] (l: List[(Key, List[ListElement])], printFunc: (Any => Unit) = println): Unit = {
    ((1 to l.size) zip l).foreach(a => printFunc(a._1 + "  " + a._2._1 + "\n" + a._2._2)) //the main thread adds msg here
  }

  val LOG_DEBUG = 0
  val LOG_INFO = 1
  val LOG_WARNING = 2
  val LOG_ERROR = 3
}
