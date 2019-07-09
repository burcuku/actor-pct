package akka.dispatch.state

import akka.actor.ActorRef
import akka.dispatch.Envelope
import akka.dispatch.TestingDispatcher.Message
import akka.dispatch.util.{CmdLineUtils, IdGenerator}
import explorer.protocol.MessageId

object Messages {
  private val idGenerator = new IdGenerator(0)
  val NO_MSG = -1
}

class Messages {
  import Messages._
  // All the messages sent in the execution
  private var messages: Map[MessageId, Message] = Map()
  private var processed: Set[MessageId] = Set(0)

  def get(id: MessageId): Message = messages(id)

  def addMessage(receiver: ActorRef, invocation: Envelope): MessageId = {
    val messageId = idGenerator.next
    CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "Added: " + messageId + "     " + invocation.message)
    messages += (messageId -> Message(messageId, receiver, invocation))
    messageId
  }

  def setProcessed(m: MessageId): Boolean =
    if(messages.keySet.contains(m)) {
      processed = processed + m
      true
    } else false

  def isProcessed(m: MessageId): Boolean = processed.contains(m)
  def getAllMessageIds: Set[MessageId] = messages.keySet
  def getAllMessages: Iterable[Message] = messages.values
}
