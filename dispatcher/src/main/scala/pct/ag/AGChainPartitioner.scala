package pct.ag

import akka.dispatch.util.IdGenerator
import com.typesafe.scalalogging.LazyLogging
import pct.{ChainId, MessageId}

//todo consider updating List to Vector

object AGChainPartitioner {
  case class Node(id: MessageId, preds: Set[MessageId]) // keeps preds just to check the invariant
  case class Chain(id: ChainId, elems: List[Node])

  type BSet = Set[Chain]
  type Partitioning = List[BSet]
}

class AGChainPartitioner extends LazyLogging {

  import AGChainPartitioner._
  val idGen = new IdGenerator(0)

  var partitioning: Partitioning = List()

  def insert(elem: Node): Unit = partitioning = insert(elem, partitioning)

  def insert(elem: Node, partition: Partitioning): Partitioning = {

    def insertIntoBSet(index: Int): List[BSet] = {
      if(partition.size <= index) {
        // Create a new BSet and add the element
        partition :+ Set(Chain(idGen.next, List(elem)))
      } else {
        val bset = partition(index)
        bset.filter(canAppendToChain(elem, _)).toList.sortBy(_.elems.last.id) match {
          // create a new chain in the BSet
          case Nil if bset.size < index + 1 =>
            partition.updated(index, bset + Chain(idGen.next, List(elem)))
          // BSet is full, move to the next BSet
          case Nil =>
            insertIntoBSet(index + 1)
          // The element fits in a single chain, append it
          case c :: Nil =>
            partition.updated(index, bset - c + appendToChain(elem, c))
          // The element fits in more than one chains, append to one of them and update B(i-1) and B(i)
          case c :: xs => // more than one chains, need to swap
            val leftChains = partition(index - 1)
            partition.updated(index-1, bset - c).updated(index, leftChains + appendToChain(elem, c))
        }
      }
    }
    val bsets = insertIntoBSet(0)
    logger.debug("Inserted: " + elem.id + " preds: " + elem.preds)
    bsets.foreach(x => logger.debug(x.toString))

    try {
      assert(bsets.indices.forall(i => bsets(i).size <= i + 1))
      assert(!bsets.indices.exists(i => hasComparableElems(getMaximals(bsets(i)))))
    } catch {
      case e: Throwable =>
        logger.error("BSet invariant is broken!\n" + e)
        println(Console.RED + "BSet invariant is broken!\n" + e + Console.RESET)
        System.exit(-1)
    }

    bsets

  }

  def appendToChain(elem: Node, chain: Chain): Chain = Chain(chain.id, chain.elems :+ elem)

  def canAppendToChain(elem: Node, chain: Chain): Boolean = chain.elems.lastOption match {
    case Some(e) if elem.preds.contains(e.id) => true
    case None => true
    case _ => false
  }

  def getChains: List[Chain] = partitioning.flatMap(bset => bset.toList).sortBy(_.id)
  def getChain(id: ChainId): Option[Chain] = partitioning.flatMap(bset => bset.toList).find(_.id == id)

  /* auxiliary to check the invariant */
  private def getMaximals(bset: BSet): Set[Node] = bset.map(_.elems.last)
  private def isComparable(elem1: Node, elem2: Node): Boolean = elem1.preds.contains(elem2.id) || elem2.preds.contains(elem1.id)
  private def hasComparableElems(elems: Set[Node]): Boolean = elems.zip(elems).filter(p => p._1 != p._2).exists(p => isComparable(p._1, p._2))

  def printPartitioning(bsets: Partitioning): Unit = {
    bsets.zip(1 to bsets.size).foreach(x => println("B " + x._1 + " : " + x._2))
  }

  def printPartitioning(): Unit = {
    partitioning.zip(1 to partitioning.size).foreach(x => println("B " + x._1 + " : " + x._2))
  }
}

