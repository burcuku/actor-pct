package pct.ag

import org.scalatest.{FlatSpec, Matchers}
import pct.{MessageId, PCTOptions}
import pct.ag.AGChainPartitioner.{Chain, Node}

class PCTSchedulerAGTest extends FlatSpec with Matchers {

  val ps = new PCTSchedulerAG(PCTOptions(randomSeed = 12345, maxMessages = 8, bugDepth = 1))

  var msgs: Map[MessageId, Set[MessageId]] = Map()
  val elems: List[MessageId] = List(0, 1, 2, 3, 4, 5, 6, 7)
  var preds: List[Set[MessageId]] = List(Set(), Set(0), Set(0), Set(0, 1), Set(2), Set(2), Set(2, 5))
  val nodes: List[Node] = elems.zip(preds).map(pair => Node(pair._1, pair._2)).sortBy(_.id)

  // create another ps with a priority change point after the first message
  val ps2 = new PCTSchedulerAG(PCTOptions(randomSeed = 12345, maxMessages = 4, bugDepth = 2))

  // schedule
  it should "return None for scheduling when empty" in {
    ps.scheduleNextMessage shouldBe None
  }

  // add element
  it should "add msg into empty chain/partitioning" in {
    ps.addNewMessages(Map(elems.head -> preds.head))

    // chain with id 0 is created
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()))))
    ps.getPriorities shouldBe List(0)
  }

  //schedule
  it should "return the only element for scheduling when has a single element" in {
    ps.next(0) shouldBe Some(nodes.head)
    ps.scheduleNextMessage shouldBe Some(0)
    ps.next(0) shouldBe None
  }

  // add element
  it should "append msg into a chain/partitioning with a single element" in {
    ps.addNewMessages(Map(elems(1) -> preds(1)))

    // inserted into chain with id 0
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)))))
    ps.getPriorities shouldBe List(0)

  }

  // add element
  it should "add a concurrent msg and have two chains/priorities" in {
    ps.addNewMessages(Map(elems(2) -> preds(2)))

    // added a chain with id 1
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)))), Chain(1, List(Node(2, Set(0)))))
    ps.getPriorities.toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  // schedule
  it should "return the next element with the highest priority when it has two chains" in {
    ps.next(0) shouldBe Some(nodes(1))
    ps.next(1) shouldBe Some(nodes(2))
    // priorities: List(1, 0)

    // random seed 12345 assigns chain 1 as the highest
    ps.scheduleNextMessage shouldBe Some(2)
    ps.next(0) shouldBe Some(nodes(1))
    ps.next(1) shouldBe None
  }

  it should "append a msg to one of the possible two chains (with id 0 in BSet1)" in {
    ps.addNewMessages(Map(elems(3) -> preds(3)))

    // inserted into chain with id 0
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)))))
    ps.getPriorities.toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  it should "append a msg to a chain (with id 1 in BSet2)" in {
    ps.addNewMessages(Map(elems(4) -> preds(4)))

    // inserted into chain with id 1
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))))
    ps.getPriorities.toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  it should "add a concurrent msg to a new chain (with id 2 in BSet2)" in {
    ps.addNewMessages(Map(elems(5) -> preds(5)))

    // inserted into chain with id 2
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))),
      Chain(2, List(Node(5, Set(2)))))
    ps.getPriorities.toSet shouldBe Set(0, 1, 2) // priorities can be set in any order
  }

  // schedule
  it should "return the next element which has the highest priority among enables events it has many chains" in {
    ps.next(2) shouldBe Some(nodes(5))
    // priorities: List(1, 0, 2)

    // random seed 12345 assigns priorities with ordering (1, 0, 2), chain2 has the highest
    ps.getPriorities shouldBe List(1, 0, 2)

    // chain1 has nodes2
    ps.next(1) shouldBe Some(nodes(4))
    ps.scheduleNextMessage shouldBe Some(4)
    //ps.next(0) shouldBe None
    //ps.next(2) shouldBe Some(nodes(5))
  }

  it should "append a msg to a chain (with id 2 in BSet2)" in {
    ps.addNewMessages(Map(elems(6) -> preds(6)))

    // inserted into chain with id 2
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))),
      Chain(2, List(Node(5, Set(2)), Node(6, Set(2, 5)))))
    ps.getPriorities.toSet shouldBe Set(0, 1, 2) // priorities can be set in any order
  }

  // schedule all the rest
  it should "after scheduling all messages, schedule should return None" in {
    // 3 messages scheduled, schedule 4 more
    ps.scheduleNextMessage.isDefined shouldBe true
    ps.scheduleNextMessage.isDefined shouldBe true
    ps.scheduleNextMessage.isDefined shouldBe true
    ps.scheduleNextMessage.isDefined shouldBe true
    ps.scheduleNextMessage shouldBe None
  }

  it should "decrease priority at priority change point" in {
    // use ps2 with a priority inversion point
    // add elements
    elems.zip(preds).sortBy(_._1).foreach(p => ps2.addNewMessages(Map(p._1 -> p._2)))

    // random seed 12345 assigns chains:
    // Chain(0,List(Node(0,Set()), Node(1,Set(0)), Node(3,Set(0, 1))))
    // Chain(1,List(Node(2,Set(0)), Node(4,Set(2))))
    // Chain(2,List(Node(5,Set(2)), Node(6,Set(2, 5))))

    // priorities: List(1, 0, 2)
    // priority change at 1

    ps2.getPriorities shouldBe List(1, 0, 2)
    ps2.getPriorityChangePts shouldBe Set(1) // change point before the message (m1) in Chain 0

    // chain1's head is node2
    ps2.next(1) shouldBe Some(nodes(2))
    ps2.scheduleNextMessage shouldBe Some(2)
    ps2.scheduleNextMessage shouldBe Some(4)

    ps2.next(0) shouldBe Some(nodes(0))
    ps2.scheduleNextMessage shouldBe Some(0)

    // chain1's head is node1 - its priority is reduced
    ps2.next(0) shouldBe Some(nodes(1))
    // hence, the next is m5 from chain2
    ps2.scheduleNextMessage shouldBe Some(5)


    // priority of chain 0 is decreased after processing the first message
    ps2.getPriorities.toSet shouldBe Set(1, 2, 0)
    // now the chain 1 has the highest priority, but has no elements, chain 2 has
    ps2.getCurrentChain shouldBe Some(2)
    // the next element is from chain2, node6
    ps2.scheduleNextMessage shouldBe Some(6)
  }

  it should "execute the messages in the chain with reduced priority after the other chains" in {

    // priority of chain 0 was decreased
    ps2.getPriorities.toSet shouldBe Set(1, 2, 0)

    // current chain with an available element is the reduced chain, i.e., chain0
    ps2.getCurrentChain shouldBe Some(0)
    // the next element in this chain is m1 (at which priority wa reduced)
    ps2.next(0) shouldBe Some(nodes(1))

    // add a new message to chain2
    ps2.addNewMessages(Map(16.asInstanceOf[MessageId] -> Set(6.asInstanceOf[MessageId])))

    // now, the current chain is chain2
    ps2.getCurrentChain shouldBe Some(2)
    // the next element in this chain is m1 (at which priority wa reduced)
    ps2.next(2) shouldBe Some(Node(16.asInstanceOf[MessageId], Set(6.asInstanceOf[MessageId])))

    // run the next element i.e., m16
    ps2.scheduleNextMessage shouldBe Some(16)

    // all messages in chain 1 and 2 are executed
    // next available message is in chain0
    // current chain with an available elemnt is the reduced chain, i.e., chain0
    ps2.getCurrentChain shouldBe Some(0)
    // the next element in this chain is m1 (at which priority wa reduced)
    ps2.next(0) shouldBe Some(nodes(1))

    //schedules the delayed element
    ps2.scheduleNextMessage shouldBe Some(1)

    //schedules the next element in the reduced priority chain
    ps2.scheduleNextMessage shouldBe Some(3)

    // all messages are executed
    ps2.getCurrentChain shouldBe None
    ps2.scheduleNextMessage shouldBe None
  }
}
