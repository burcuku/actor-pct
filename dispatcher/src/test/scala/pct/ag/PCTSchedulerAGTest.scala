package pct.ag

import org.scalatest.{FlatSpec, Matchers}
import pct.{MessageId, PCTOptions}
import pct.ag.ChainPartitioner.{Chain, Node}

class PCTSchedulerAGTest extends FlatSpec with Matchers {

  val ps = new PCTSchedulerAG(PCTOptions(randomSeed = 12345, maxMessages = 8, bugDepth = 1))

  var msgs: Map[MessageId, Set[MessageId]] = Map()
  val elems: List[MessageId] = List(0, 1, 2, 3, 4, 5, 6, 7)
  var preds: List[Set[MessageId]] = List(Set(), Set(0), Set(0), Set(0, 1), Set(2), Set(2), Set(2, 5))
  val nodes: List[Node] = elems.zip(preds).map(pair => Node(pair._1, pair._2)).sortBy(_.id)

  // schedule
  it should "return None for scheduling when empty" in {
    ps.getNextMessage shouldBe None
  }

  // add element
  it should "add msg into empty chain/partitioning" in {
    ps.addNewMessages(Map(elems.head -> preds.head))

    // chain with id 0 is created
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()))))
    ps.getPriorities(true) shouldBe List(0)
  }

  //schedule
  it should "return the only element for scheduling when has a single element" in {
    ps.next(0) shouldBe Some(nodes.head)
    ps.getNextMessage shouldBe Some(0)
    ps.next(0) shouldBe None
  }

  // add element
  it should "append msg into a chain/partitioning with a single element" in {
    ps.addNewMessages(Map(elems(1) -> preds(1)))

    // inserted into chain with id 0
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)))))
    ps.getPriorities(true) shouldBe List(0)

  }

  // add element
  it should "add a concurrent msg and have two chains/priorities" in {
    ps.addNewMessages(Map(elems(2) -> preds(2)))

    // added a chain with id 1
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)))), Chain(1, List(Node(2, Set(0)))))
    ps.getPriorities(true).toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  // schedule
  it should "return the next element with the highest priority when it has two chains" in {
    ps.next(0) shouldBe Some(nodes(1))
    ps.next(1) shouldBe Some(nodes(2))

    // random seed 12345 assigns chain 0 as the highest
    ps.getNextMessage shouldBe Some(1)
    ps.next(0) shouldBe None
  }

  it should "append a msg to one of the possible two chains (with id 0 in BSet1)" in {
    ps.addNewMessages(Map(elems(3) -> preds(3)))

    // inserted into chain with id 0
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)))))
    ps.getPriorities(true).toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  it should "append a msg to a chain (with id 1 in BSet2)" in {
    ps.addNewMessages(Map(elems(4) -> preds(4)))

    // inserted into chain with id 1
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))))
    ps.getPriorities(true).toSet shouldBe Set(0, 1) // priorities can be set in any order
  }

  it should "add a concurrent msg to a new chain (with id 2 in BSet2)" in {
    ps.addNewMessages(Map(elems(5) -> preds(5)))

    // inserted into chain with id 2
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))),
      Chain(2, List(Node(5, Set(2)))))
    ps.getPriorities(true).toSet shouldBe Set(0, 1, 2) // priorities can be set in any order
  }

  // schedule
  it should "return the next element which has the highest priority among enables events it has many chains" in {
    ps.next(2) shouldBe Some(nodes(5))

    // random seed 12345 assigns priorities with ordering (2, 0, 1), chain2 has the highest
    ps.getPriorities(true) shouldBe List(2, 0, 1)

    // chain2 has node5 as its next, not enabled
    ps.next(2) shouldBe Some(nodes(5))
    // node3 of chain0 will be scheduled
    ps.getNextMessage shouldBe Some(3)
    ps.next(0) shouldBe None
    ps.next(2) shouldBe Some(nodes(5))
  }

  it should "append a msg to a chain (with id 2 in BSet2)" in {
    ps.addNewMessages(Map(elems(6) -> preds(6)))

    // inserted into chain with id 2
    ps.getChains shouldBe List(Chain(0, List(Node(0, Set()), Node(1, Set(0)), Node(3, Set(0, 1)))), Chain(1, List(Node(2, Set(0)), Node(4, Set(2)))),
      Chain(2, List(Node(5, Set(2)), Node(6, Set(2, 5)))))
    ps.getPriorities(true).toSet shouldBe Set(0, 1, 2) // priorities can be set in any order
  }

  // schedule all the rest
  it should "after scheduling all messages, schedule should return None" in {
    // 3 messages scheduled, schedule 4 more
    ps.getNextMessage.isDefined shouldBe true
    ps.getNextMessage.isDefined shouldBe true
    ps.getNextMessage.isDefined shouldBe true
    ps.getNextMessage.isDefined shouldBe true
    ps.getNextMessage shouldBe None
  }

  it should "decrease priority at priority change point" in {
    // create another ps with a priority change point after the first message
    val ps = new PCTSchedulerAG(PCTOptions(randomSeed = 12345, maxMessages = 1, bugDepth = 2))
    // add elements
    elems.zip(preds).sortBy(_._1).foreach(p => ps.addNewMessages(Map(p._1 -> p._2)))
    //println(ps.getChains)

    // random seed 12345 assigns:
    ps.getPriorities(true) shouldBe List(2, 0, 1)
    ps.getPriorityChangePts shouldBe Set(0) // change point after the first message (0)

    // schedule the first element
    // chain2's head node5 is not enabled
    ps.next(2) shouldBe Some(nodes(5))
    // chain0's head node0 will be scheduled
    ps.getNextMessage shouldBe Some(0)
    ps.next(0) shouldBe Some(nodes(1))

    // priority of chain 0 is decreased after processing the first message
    ps.getPriorities(high = true).toSet shouldBe Set(2, 1)
    ps.getPriorities(high = false).toSet shouldBe Set(0)
    // now the chain 0 has the highest priority
    ps.getHighestPriorityChain(true) shouldBe 2
    // the next element is from chain1, node2
    ps.getNextMessage shouldBe Some(2)
  }
}
