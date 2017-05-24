package pct

import scala.collection.mutable.{ListBuffer, Map, Queue}
import scala.util.Random

object PCTOptions {
  val randSeed = System.currentTimeMillis()
  val maxChains = 0
  val maxMessages = 0
  val bugDepth = 2
}

final class Chain (var head: Option[Message] = None, var tail: Option[Message] = None) {
  
  var current = head
  while (current != None) {
    current.get.chain = Some(this)
    current = current.get.chainNext
  }

  def append(msg: Message) = {
    tail match {
      case None => head = Some(msg)
      case Some(t) =>
        if (t.isReceived)
          head = Some(msg)
        else 
          t.chainNext = Some(msg)
    }
    tail = Some(msg)
    msg.chain = Some(this) 
  }
  
  def remove(): Message = {
    if (head.get == tail.get)
      return head.get
    val rMsg = head.get
    head = head.get.chainNext
    return rMsg
  }
}

final class Message (var receiver: String, 
                     var sender: String, 
                     var payload: Any, 
                     var isReceived: Boolean = false, 
                     var preds: List[Message], 
                     var chain: Option[Chain] = None, 
                     var chainNext: Option[Message] = None) {
  
  def addPred(msg: Message) = {
    preds = preds :+ msg  
  }
  
  def allPreds(): List[Message] = {
    val before: ListBuffer[Message] = ListBuffer()
    val visited : ListBuffer[Message] = ListBuffer()
    val q: Queue[Message] = Queue(preds: _*)
    
    while (!q.isEmpty) {
      var e = q(0)
      q.dequeue()
      if (!visited.contains(e)) {
        before += e
        visited += e
        q ++: e.preds
      }
    }
    
    return before.toList    
  }
  
  def before(msg: Message): Boolean = {
    return allPreds().contains(msg)
  } 
  
  def isEnabled(): Boolean = {
    if (isReceived) return false
    allPreds().foreach(p => if (!p.isReceived) return false)
    return true
    
    //var enabled = true
    //allPreds().foreach(p => enabled = enabled && p.isReceived)
    //return enabled
  }
}

object PCTDecomposition  {  
  val chains: ListBuffer[Chain] = ListBuffer()
  private val tempChains: ListBuffer[Chain] = ListBuffer()
  private var suffixLen: Int = 0
  
  private val randInt = new Random(PCTOptions.randSeed)

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
  
  def reduce(a: Message, b: Message, pairs: Map[Message, Tuple2[Message, Message]]): Unit = {
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
    val pairs: Map[Message, Tuple2[Message, Message]] = Map()
    
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
    return false
  }
  
  def getMinEnabledMessage(): Message = {
    while (merge()) {}
        
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

object PCTStrategy {
    private val prioInvPoints: ListBuffer[Int] = ListBuffer()
    private var msgIndex: Int = 0
    private val randInt = new Random(PCTOptions.randSeed)
    
    for (i <- 0 to PCTOptions.bugDepth - 1)
      prioInvPoints += randInt.nextInt(PCTOptions.maxMessages) 
    
    def getNextMessage(): Message = {
      val nextMsg = PCTDecomposition.getMinEnabledMessage()
      if (prioInvPoints.contains(msgIndex))
        PCTDecomposition.decreasePriority(nextMsg.chain.get)
      msgIndex += 1
      nextMsg
    }
}
