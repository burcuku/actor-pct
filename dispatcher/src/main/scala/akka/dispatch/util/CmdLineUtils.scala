package akka.dispatch.util

import akka.dispatch.DispatcherUtils
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.List

object CmdLineUtils {

  def parseInput(range: Range = Range(Int.MinValue, Int.MaxValue), allowedStrs: List[String] = List()): (String, Option[Int]) = {
    val line = Console.readLine().split(" ")

    if (line.isEmpty || !allowedStrs.contains(line(0))) {
      println("Wrong input, try again. ")
      return parseInput(range, allowedStrs)
    }

    if(line(0).equalsIgnoreCase("start") || line(0).equalsIgnoreCase("quit")) (line(0), None)
    else if(line.size != 2 || !line(1).charAt(0).isDigit) {
      println("Wrong input, try again. ")
      parseInput(range, allowedStrs)
    }
    else if (line(1).toInt < range.start || line(1).toInt > range.end) {
      println("Wrong integer input, try again. ")
      parseInput(range, allowedStrs)
    }
    else {
      (line(0), Some(line(1).toInt))
    }
  }

  val logging: Boolean = try {
    ConfigFactory.load(DispatcherUtils.debuggerConfigFile).getBoolean("dispatcher.dispatcherLogs")
  } catch {
    case e: Exception => true // logging is on by default
  }

  //todo organize
  def printlnForInfo(s: Any): Unit = {
    println(Console.GREEN + s + Console.RESET)
  }

  def printlnForLogging(s: Any): Unit = {
    if(logging) println(Console.CYAN + s + Console.RESET)
  }

  def printlnForWELogging(s: Any): Unit = {
    println(Console.MAGENTA + s + Console.RESET)
  }

  def printlnForUiInput(s: Any): Unit = {
    println(Console.BLUE + s + Console.RESET)
  }

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
}
