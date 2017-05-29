package pct

import scala.collection.mutable.{ListBuffer, Map}
import scala.util.Random

/*
class PCTDecomposition(options: PCTOptions) {
  val chains: ListBuffer[Chain] = ListBuffer()
  private val tempChains: ListBuffer[Chain] = ListBuffer()
  private var suffixLen: Int = 0

  private val randInt = new Random(options.randomSeed)

  //Messages are placed in chains (tempChains) greedily.
  def extend(msgs: List[Message]): Unit = {
    msgs.foreach {
      msg =>
        chains.foreach {
          c =>
            if (msg.before(c.tail.get)) {
              c.append(msg)
              return
            }
        }
        tempChains.foreach {
          tc =>
            if (msg.before(tc.tail.get)) {
              tc.append(msg)
              return
            }
        }
        tempChains += new Chain(Some(msg), Some(msg))
    }
  }

  def rewrite(a: Message, b: Message) = {
    a.chainNext = Some(b)
    a.chain.get.tail = b.chain.get.tail
    var current: Option[Message] = Some(b)
    while (current != None) {
      current.get.chain = a.chain
      current = current.get.chainNext
    }
  }

  def reduce(a: Message, b: Message, pairs: Map[Message, (Message, Message)]): Unit = {
    var ma = a
    var mb = b

    while (true) {
      val bChain = mb.chain
      rewrite(ma, mb)
      if (!pairs.contains(mb)) {
        //chains = chains.filter(_ != bChain.get)
        chains -= bChain.get
        tempChains -= bChain.get
        return
      }
      ma = pairs(b)._1
      mb = pairs(b)._2
    }
  }

  //Reducing the total number of chains by one.
  def merge() : Boolean = {
    var currentQ: ListBuffer[Message] = ListBuffer()
    var nextQ: ListBuffer[Message] = ListBuffer()
    val pairs: Map[Message, (Message, Message)] = Map()

    chains.foreach(c => currentQ += c.head.get)
    tempChains.foreach(tc => currentQ += tc.head.get)

    while (!currentQ.isEmpty) {
      currentQ.foreach {
        b =>
          b.allPreds().foreach {
            a =>
              val aNext = a.chainNext
              if (aNext == None) {
                reduce(a, b, pairs)
                return true
              }
              if (!pairs.contains(aNext.get)) {
                pairs(aNext.get) = (a, b)
                nextQ += aNext.get
              }
          }
      }
      //currentQ = nextQ
      currentQ.clear
      currentQ ++= nextQ
      nextQ.clear
    }
    false
  }

  def getMinEnabledMessage(): Message = {

    while (merge()) {

    }
    tempChains.foreach {
      tc =>
        val pos = randInt.nextInt(chains.size - suffixLen)
        chains.insert(pos, tc)
    }
    tempChains.clear

    chains.foreach {
      c =>
        if (c.head.get.isEnabled) {
          c.head.get.isReceived = true
          return c.remove()
        }
    }
    return null
  }

  def decreasePriority(chain: Chain) = {
    chains.remove(chains.indexOf(chain))
    chains += chain
    suffixLen += 1
  }
}
*/