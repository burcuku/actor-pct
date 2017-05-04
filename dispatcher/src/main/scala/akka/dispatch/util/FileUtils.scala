package akka.dispatch.util

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable.ListBuffer
import scala.io.Source

object FileUtils {

  def printToFile(fileName: String)(op: java.io.PrintWriter => Unit) {

    val path: Path = Paths.get("out")
    if (!Files.exists(path)) {
      val success = new File("out").mkdirs();
      if(!success) {
        System.err.println("Cannot create the output directory")
        return
      }
    }

    val file = new File("out/" + fileName + new SimpleDateFormat("MMdd-HHmmss'.txt'").format(new Date()))
    val p = new java.io.PrintWriter(file)
    try { op(p) } finally { p.close() }
  }

  /**
    * @param fileName the name of the file
    * @return the list of lines
    */
  def readFromFile(fileName: String): List[String] = {
    var list: ListBuffer[String] = ListBuffer()
    for (line <- Source.fromFile(fileName).getLines()) {
      list += line
    }
    list.toList
  }

}
