package pct.bm

import pct.{Message, MessageId}

import scala.annotation.tailrec
import scala.collection.mutable

class Messages {
  private val idsToMsgs: mutable.Map[MessageId, Message] = mutable.Map()

  def putMessage(message: Message): Unit = idsToMsgs += (message.id -> message)

  def getMessage(id: MessageId): Message = idsToMsgs(id)
  
  def allPreds(id: MessageId): Set[MessageId] = {

    @tailrec
    def visitPreds(toVisit: List[MessageId], visited: Set[MessageId], result: Set[MessageId]): Set[MessageId] =
      toVisit match {
        case Nil => result
        case x :: xs if visited.contains(x) => visitPreds(xs, visited, result)
        case x :: xs => visitPreds(xs ++ idsToMsgs(x).preds.toList, visited + x, result + x)
      }

    visitPreds(idsToMsgs(id).preds.toList, Set(), Set())
  }

  def isBefore(id: MessageId, other: MessageId): Boolean = allPreds(other).contains(id)

  //def isEnabled(msg: Message): Boolean = !msg.received && msg.preds.map(id => idsToMsgs(id)).forall(_.received)
  
  def isEnabled(id: MessageId): Boolean = !idsToMsgs(id).received && idsToMsgs(id).preds.map(prevId => idsToMsgs(prevId)).forall(_.received)  
  
  def markReceived(id: MessageId) = idsToMsgs(id).received = true
}
