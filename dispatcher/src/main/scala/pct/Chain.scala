package pct

import scala.collection.mutable

class Chain {

  private val messages: mutable.ListBuffer[MessageId] = mutable.ListBuffer.empty

  def this(id: MessageId) {
    this()
    messages += id  
  }
    
  def append(id: MessageId): Unit = messages += id
  
  def remove(id: MessageId): Unit = messages -= id
  
  def appendAll(ids: List[MessageId]): Unit = messages ++= ids
  
  def removeAll(ids: List[MessageId]): Unit = messages --= ids
  
  def sliceSuccessors(id: MessageId): List[MessageId] = messages.slice(messages.indexOf(id), messages.length).toList 
  
  def nextMessage(id: MessageId): Option[MessageId] = {
    require(messages.contains(id))
    val idx = messages.indexOf(id)
    if (idx == messages.length - 1) 
      None
    else 
      Some(messages(idx + 1))
  }
  
  def contains(id: MessageId): Boolean = messages.contains(id)
  
  def head: Option[MessageId] = messages.headOption
  
  def tail: Option[MessageId] = messages.lastOption

  //def firstUnreceived: Option[Message] = messages.find(msg => !msg.received)
  
  def firstEnabled: Option[MessageId] = messages.find(id => Messages.isEnabled(id))
  
  def toList: List[MessageId] = messages.toList
}
