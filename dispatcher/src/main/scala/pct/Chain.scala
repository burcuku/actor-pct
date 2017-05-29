package pct

import scala.collection.mutable

class Chain {

  private val messages: mutable.ListBuffer[Message] = mutable.ListBuffer.empty

  def append(message: Message): Unit = messages += message

  def head: Option[Message] = messages.headOption
  
  def tail: Option[Message] = messages.lastOption

  def firstUnreceived: Option[Message] = messages.find(msg => !msg.received)
}
