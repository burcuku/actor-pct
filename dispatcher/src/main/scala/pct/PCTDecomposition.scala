package pct

import scala.collection.mutable
import scala.util.Random
import scala.annotation.tailrec

class PCTDecomposition(options: PCTOptions) {
  type ReducingSeq = Tuple3[MessageId, MessageId, Map[MessageId, (MessageId, MessageId)]]

  private val chains: mutable.ListBuffer[Chain] = mutable.ListBuffer.empty  
  private val tempChains: mutable.ListBuffer[Chain] = mutable.ListBuffer.empty
  private val randInt = new Random //new Random(options.randomSeed)
  private var suffixLen: Int = 0

  private def msgToChain(id: MessageId): Chain = {
    chains.find(_.contains(id)) match {
      case Some(c) => c
      case None => tempChains.find(_.contains(id)).get
    }
  }
  
  private def nextMessage(id: MessageId): Option[MessageId] = msgToChain(id).nextMessage(id)
    
  //Reducing the total number of chains by one.
  private def reduce(): Boolean = {
      
    @tailrec
    def redoChains(pid: MessageId, id: MessageId, pairs: Map[MessageId, (MessageId, MessageId)]): Unit = {
      require(Messages.isBefore(pid, id))
      
      val pidChain = msgToChain(pid)
      val idChain = msgToChain(id)
      val ids = idChain.sliceSuccessors(id)
      pidChain.appendAll(ids)
      idChain.removeAll(ids)
      
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
          val preds = Messages.allPreds(id)
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
  
  def extend(ids: List[MessageId]) {    
    //Messages are placed in chains greedily.
    def placeInChains(id: MessageId, inputChains: mutable.ListBuffer[Chain]): Boolean = 
      inputChains.find(c => Messages.isBefore(c.tail.get, id)) match {
        case Some(c) => 
          c.append(id)
          true
        case None => false
    }
    
    for (id <- ids if !placeInChains(id, chains) && !placeInChains(id, tempChains)) {
        tempChains += new Chain(id)      
    }    
  }
  
  def minimizeChains: Unit = if (reduce()) minimizeChains
  
  def shuffleChains = {
    tempChains.foreach {
      tc => tc.head match {
        case Some(_) => 
          val newIndx = randInt.nextInt(chains.length + 1 - suffixLen)
          chains.insert(newIndx, tc)
        case None =>
      }
    }
    tempChains.clear    
  }
  
  def getMinEnabledMessage(): Option[MessageId] = {    
    //minimizeChains
    //shuffleChains    
    chains.find(_.firstEnabled != None) match {
      case Some(c) =>
        val enabledMessage = c.firstEnabled
        Messages.markReceived(enabledMessage.get)
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
