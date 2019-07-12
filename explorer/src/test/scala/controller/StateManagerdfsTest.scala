package controller

import explorer.protocol.{MessageId, MessageSent, ProgramEvent}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class StateManagerdfsTest extends FlatSpec with Matchers {

  val event1 = MessageSent("s1", "s2", "Syn")
  val event2 = MessageSent("s1", "s2", "Synpll")
  val event3 = MessageSent("s1", "s3", "Syn")
  var messageId: MessageId = 0

  val eventList: ListBuffer[(MessageId, ProgramEvent)] = new ListBuffer[(MessageId, ProgramEvent)]()
  eventList.insert(0, (1, event1))
  eventList.insert(0, (2, event2))
  var predecessors: Map[MessageId, Set[MessageId]] = Map()

  predecessors+= (messageId+1)-> Set(0)
  predecessors+= (messageId+2) -> Set(0)


  it should "handle the case of (no additional events and no messages left)" in {
    val stateManagerdfs: StateManagerdfs = new StateManagerdfs

    stateManagerdfs.addNewMessages(List(), Map())
    assert(stateManagerdfs.scheduleNextMessage.isEmpty) // Since no other message is there

  }

  it should "handle the case of additional events and should emit them properly)" in {

    val stateManagerdfs: StateManagerdfs = new StateManagerdfs

    stateManagerdfs.addNewMessages(eventList.toList, predecessors)

    var nextM: Option[MessageId] = stateManagerdfs.scheduleNextMessage
    stateManagerdfs.addNewMessages(List(), Map())
    assert(nextM.isDefined && nextM.get == 2)

    nextM = stateManagerdfs.scheduleNextMessage
    stateManagerdfs.addNewMessages(List(), Map())
    assert(nextM.isDefined && nextM.get == 1)
//
    nextM = stateManagerdfs.scheduleNextMessage
    assert(nextM.isEmpty)
    stateManagerdfs.addNewMessages(List(), Map())

    nextM = stateManagerdfs.scheduleNextMessage
    assert(nextM.isDefined && nextM.get == 1)
    stateManagerdfs.addNewMessages(List(), Map())

    nextM = stateManagerdfs.scheduleNextMessage
    assert(nextM.isDefined && nextM.get == 2)
    stateManagerdfs.addNewMessages(List(), Map())

    nextM = stateManagerdfs.scheduleNextMessage
    assert(nextM.isEmpty)
    stateManagerdfs.addNewMessages(List(), Map())

  }





}
