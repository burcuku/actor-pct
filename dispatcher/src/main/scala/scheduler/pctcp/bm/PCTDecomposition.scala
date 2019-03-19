package scheduler.pctcp.bm

import pctcp.PCTCPOptions
import protocol.{Message, MessageId}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Random

class PCTDecomposition {
  type ReducingSeq = Tuple3[MessageId, MessageId, Map[MessageId, (MessageId, MessageId)]]

  private val messagesDependencies: Messages = new Messages()
  private val chains: mutable.ListBuffer[Chain] = mutable.ListBuffer.empty  
  private val tempChains: mutable.ListBuffer[Chain] = mutable.ListBuffer.empty
  private val randInt = new Random//new Random(options.randomSeed)
  private var suffixLen: Int = 0
  private val msgToChainMap: mutable.Map[MessageId, Chain] = mutable.Map()
  private val nextMessageMap: mutable.Map[MessageId, MessageId] = mutable.Map()
  
  private def msgToChain(id: MessageId): Chain = {
    msgToChainMap(id)
    /*chains.find(_.contains(id)) match {
      case Some(c) => c
      case None => tempChains.find(_.contains(id)).get
    }*/
  }
  
  private def nextMessage(id: MessageId): Option[MessageId] = nextMessageMap.get(id) //msgToChain(id).nextMessage(id)
    
  //Reducing the total number of chains by one.
  private def reduce(): Boolean = {
      
    @tailrec
    def redoChains(pid: MessageId, id: MessageId, pairs: Map[MessageId, (MessageId, MessageId)]): Unit = {
      require(messagesDependencies.isBefore(pid, id))
      
      val pidChain = msgToChain(pid)
      val idChain = msgToChain(id)
      val ids = idChain.sliceSuccessors(id)
      pidChain.appendAll(ids)
      idChain.removeAll(ids)
      ids.foreach(id => msgToChainMap += (id -> pidChain))
      nextMessageMap += (pid -> id)
      idChain.tail match {
        case Some(t) => nextMessageMap -= t
        case None =>
      }
      
      if (!pairs.contains(id)) {
        chains -= idChain
        tempChains -= idChain
      }
      else redoChains(pairs(id)._1, pairs(id)._2, pairs)       
    }
    
    @tailrec
    def findReducingSeq(currentQ: List[MessageId], pairs:  Map[MessageId, (MessageId, MessageId)]): Option[ReducingSeq] = {
      currentQ match {
        case Nil => None
        case id :: ids =>
          val preds = messagesDependencies.allPreds(id)
          preds.find(nextMessage(_) == None) match {
            case Some(pid) =>
              Some((pid, id, pairs))             
            case None =>
              val temp = preds.filter(pid => !pairs.contains(nextMessage(pid).get))
              findReducingSeq(ids ++ temp.map(nextMessage(_).get), 
                          pairs ++ temp.map(pid => (nextMessage(pid).get -> (pid, id))))
          }
      }    
      
    }
        
    val currentQ = chains.filter(_.head != None).map(_.head.get).toList ++ tempChains.filter(_.head != None).map(_.head.get).toList
    val pairs: Map[MessageId, (MessageId, MessageId)] = Map()
    
    findReducingSeq(currentQ, pairs) match {
      case Some(rs) => 
        redoChains(rs._1, rs._2, rs._3)
        true
      case None => false
      
    }
  }
    
  def getChains = chains.toList ++ tempChains.toList
  
  def putMessages(predecessors: Map[MessageId, Set[MessageId]]) = 
    predecessors.foreach(m => messagesDependencies.putMessage(new Message(m._1, m._2)))

  def extend(ids: List[MessageId]) = {    
    //Messages are placed in chains greedily.
    def placeInChains(id: MessageId, inputChains: mutable.ListBuffer[Chain]): Boolean = 
      inputChains.find(c => messagesDependencies.isBefore(c.tail.get, id)) match {
        case Some(c) => 
          val prevTail = c.tail.get
          c.append(id)
          msgToChainMap += (id -> c)
          nextMessageMap += (prevTail -> id)
          true
        case None => false
    }
    
    for (id <- ids if !placeInChains(id, chains) && !placeInChains(id, tempChains)) {
      val newChain = new Chain(id) 
      tempChains += newChain
      msgToChainMap += (id -> newChain)  
    }    
  }
  
  def minimizeChains: Unit = if (reduce()) minimizeChains
  
  def shuffleChains = {
    tempChains.foreach {
      tc => tc.head match {
        case Some(_) => 
          val newIndx = randInt.nextInt(chains.length + 1 - suffixLen)
          //println(newIndx)
          chains.insert(newIndx, tc)
        case None =>
      }
    }
    tempChains.clear    
  }
  
  def getMinEnabledMessage(): Option[MessageId] = {    
    //minimizeChains
    //shuffleChains    
    
    def firstEnabled:  Option[Chain] = {
      chains.find(c => c.toList.find(id => messagesDependencies.isEnabled(id)) != None)
    }
    firstEnabled match {
      case Some(c) =>
        val enabledMessage = c.toList.find(id => messagesDependencies.isEnabled(id))
        messagesDependencies.markReceived(enabledMessage.get)
        enabledMessage
      case None => None
    }
  }

  def decreasePriority(id: MessageId) = {
    val chain = msgToChain(id)
    assert(chains.contains(chain))
    chains -= chain
    chains += chain
    suffixLen += 1
  }
}
