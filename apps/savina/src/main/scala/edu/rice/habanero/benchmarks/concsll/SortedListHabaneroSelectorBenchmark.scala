package edu.rice.habanero.benchmarks.concsll

import java.util.Random

import edu.rice.habanero.actors.{HabaneroActor, HabaneroSelector}
import edu.rice.habanero.benchmarks.concsll.SortedListConfig.{DoWorkMessage, EndWorkMessage}
import edu.rice.habanero.benchmarks.{Benchmark, BenchmarkRunner}
import edu.rice.hj.Module0._
import edu.rice.hj.api.HjSuspendable

/**
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
object SortedListHabaneroSelectorBenchmark {

  def main(args: Array[String]) {
    BenchmarkRunner.runBenchmark(args, new SortedListHabaneroSelectorBenchmark)
  }

  private final class SortedListHabaneroSelectorBenchmark extends Benchmark {
    def initialize(args: Array[String]) {
      SortedListConfig.parseArgs(args)
    }

    def printArgInfo() {
      SortedListConfig.printArgs()
    }

    def runIteration() {
      finish(new HjSuspendable {
        override def run() = {
          val numWorkers: Int = SortedListConfig.NUM_ENTITIES
          val numMessagesPerWorker: Int = SortedListConfig.NUM_MSGS_PER_WORKER

          val master = new Master(numWorkers, numMessagesPerWorker)
          master.start()
        }
      })
    }

    def cleanupIteration(lastIteration: Boolean, execTimeMillis: Double) {
    }
  }

  private class Master(numWorkers: Int, numMessagesPerWorker: Int) extends HabaneroActor[AnyRef] {

    private final val workers = new Array[HabaneroActor[AnyRef]](numWorkers)
    private final val sortedList = new SortedList()
    private var numWorkersTerminated: Int = 0

    override def onPostStart() {
      sortedList.start()

      var i: Int = 0
      while (i < numWorkers) {
        workers(i) = new Worker(this, sortedList, i, numMessagesPerWorker)
        workers(i).start()
        workers(i).send(DoWorkMessage.ONLY)
        i += 1
      }
    }

    override def process(msg: AnyRef) {
      if (msg.isInstanceOf[SortedListConfig.EndWorkMessage]) {
        numWorkersTerminated += 1
        if (numWorkersTerminated == numWorkers) {
          sortedList.send(0, EndWorkMessage.ONLY)
          exit()
        }
      }
    }
  }

  private class Worker(master: Master, sortedList: SortedList, id: Int, numMessagesPerWorker: Int) extends HabaneroActor[AnyRef] {

    private final val writePercent = SortedListConfig.WRITE_PERCENTAGE
    private final val sizePercent = SortedListConfig.SIZE_PERCENTAGE
    private var messageCount: Int = 0
    private final val random = new Random(id + numMessagesPerWorker + writePercent + sizePercent)

    override def process(msg: AnyRef) {
      messageCount += 1
      if (messageCount <= numMessagesPerWorker) {
        val anInt: Int = random.nextInt(100)
        if (anInt < sizePercent) {
          sortedList.send(0, new SortedListConfig.SizeMessage(this))
        } else if (anInt < (sizePercent + writePercent)) {
          sortedList.send(0, new SortedListConfig.WriteMessage(this, random.nextInt))
        } else {
          sortedList.send(0, new SortedListConfig.ContainsMessage(this, random.nextInt))
        }
      } else {
        master.send(EndWorkMessage.ONLY)
        exit()
      }
    }
  }

  private class SortedList extends HabaneroSelector[AnyRef](1) {

    private[concsll] final val dataList = new SortedLinkedList[Integer]

    override def process(msg: AnyRef) {
      msg match {
        case writeMessage: SortedListConfig.WriteMessage =>
          val value: Int = writeMessage.value
          dataList.add(value)
          val sender = writeMessage.sender.asInstanceOf[HabaneroActor[AnyRef]]
          sender.send(new SortedListConfig.ResultMessage(this, value))
        case containsMessage: SortedListConfig.ContainsMessage =>
          val value: Int = containsMessage.value
          val result: Int = if (dataList.contains(value)) 1 else 0
          val sender = containsMessage.sender.asInstanceOf[HabaneroActor[AnyRef]]
          sender.send(new SortedListConfig.ResultMessage(this, result))
        case readMessage: SortedListConfig.SizeMessage =>
          val value: Int = dataList.size
          val sender = readMessage.sender.asInstanceOf[HabaneroActor[AnyRef]]
          sender.send(new SortedListConfig.ResultMessage(this, value))
        case _: SortedListConfig.EndWorkMessage =>
          printf(BenchmarkRunner.argOutputFormat, "List Size", dataList.size)
          exit()
        case _ =>
          System.err.println("Unsupported message: " + msg)
      }
    }
  }

}
