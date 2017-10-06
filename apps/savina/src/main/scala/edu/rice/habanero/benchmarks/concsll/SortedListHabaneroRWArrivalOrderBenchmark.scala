package edu.rice.habanero.benchmarks.concsll

import edu.rice.habanero.benchmarks.BenchmarkRunner
import edu.rice.hj.Module0._
import edu.rice.hj.api.HjSuspendable
import edu.rice.hj.experimental.actors.ReaderWriterPolicy

/**
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
object SortedListHabaneroRWArrivalOrderBenchmark {

  def main(args: Array[String]) {
    BenchmarkRunner.runBenchmark(args, new SortedListHabaneroRWArrivalOrderBenchmark)
  }

  private final class SortedListHabaneroRWArrivalOrderBenchmark extends SortedListHabaneroRWAbstractBenchmark.SortedListHabaneroRWAbstractBenchmark {

    def runIteration() {
      finish(new HjSuspendable {
        override def run() = {
          val numWorkers: Int = SortedListConfig.NUM_ENTITIES
          val numMessagesPerWorker: Int = SortedListConfig.NUM_MSGS_PER_WORKER

          val master = new SortedListHabaneroRWAbstractBenchmark.Master(numWorkers, numMessagesPerWorker, ReaderWriterPolicy.ARRIVAL_ORDER)
          master.start()
        }
      })
    }
  }

}
