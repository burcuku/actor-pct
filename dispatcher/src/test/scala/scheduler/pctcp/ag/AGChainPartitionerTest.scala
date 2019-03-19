package scheduler.pctcp.ag

import org.scalatest.{FlatSpec, Matchers}
import scheduler.pctcp.MessageId
import scheduler.pctcp.ag.AGChainPartitioner.{Chain, Node, Partitioning}

class AGChainPartitionerTest extends FlatSpec with Matchers {

  val node1 = Node(1, Set())
  val node2 = Node(2, Set())
  val node3 = Node(3, Set())
  val node4 = Node(4, Set())
  val node5 = Node(5, Set())

  val nodeDependentTo1 = Node(101, Set(1))
  val nodeDependentTo1And2 = Node(112, Set(1, 2))
  val nodeDependentTo3 = Node(103, Set(3))
  val nodeDependentTo2And4 = Node(124, Set(2, 4))
  val nodeDependentTo3And4 = Node(134, Set(3, 4))
  val nodeDependentTo4 = Node(104, Set(4))
  val nodeDependentTo4And5 = Node(145, Set(4, 5))

  val nodeConcurrent = Node(100, Set())

  val emptyChain = Chain(0, List())

  it should "calculate whether an element can be appended to an empty chain" in {
    val chain = Chain(1, List())

    val preds: Set[MessageId] = Set()

    val cp = new AGChainPartitioner()
    cp.canAppendToChain(node1, chain) shouldBe true
  }

  it should "calculate whether an element can be appended to a chain correctly when the element has no predecessors" in {
    val chain = Chain(1, List(node1, node2, node3))

    val cp = new AGChainPartitioner()
    cp.canAppendToChain(nodeConcurrent, chain) shouldBe false
  }

  it should "calculate whether an element can be appended to a chain correctly when the element has predecessors - positive" in {
    val chain = Chain(1, List(node1, node2, node3))

    val cp = new AGChainPartitioner()
    cp.canAppendToChain(nodeDependentTo3And4, chain) shouldBe true
  }

  it should "calculate whether an element can be appended to a chain correctly when the element has predecessors - negative - has more dep." in {
    val chain = Chain(1, List(node1, node2, node3))

    val cp = new AGChainPartitioner()
    cp.canAppendToChain(nodeDependentTo2And4, chain) shouldBe false
  }

  it should "calculate whether an element can be appended to a chain correctly when the element has predecessors - negative - has less dep." in {
    val chain = Chain(1, List(node1, node2, node3))

    val cp = new AGChainPartitioner()
    cp.canAppendToChain(nodeDependentTo1And2, chain) shouldBe false
  }

  it should "insert an element to an empty chain correctly" in {

    new AGChainPartitioner().appendToChain(nodeConcurrent, emptyChain) shouldBe Chain(0, List(nodeConcurrent))
  }

  it should "insert an element to a non-empty chain correctly" in {
    val chain = Chain(1, List(node1, node2, node3))

    val cp = new AGChainPartitioner()
    cp.appendToChain(nodeDependentTo3, chain) shouldBe Chain(1, List(node1, node2, node3, nodeDependentTo3))
  }

  /** add into a bset **/

  it should "add a BSet - create BSet(0) in the empty partitioning" in {
    val partitioning = List()

    new AGChainPartitioner().insert(nodeConcurrent, partitioning) shouldBe List(Set(Chain(0, List(nodeConcurrent))))
  }

  it should "add a new BSet - BSet(1)" in {
    val bset1 = Set(Chain(1, List(node1)))
    val partitioning = List(bset1)

    val cp = new AGChainPartitioner()
    cp.insert(nodeDependentTo3And4, partitioning) shouldBe List(bset1, Set(Chain(0, List(nodeDependentTo3And4))))
  }

  it should "add a new BSet - BSet(2)" in {
    val bset1 = Set(Chain(1, List(node1)))
    val bset2 = Set(Chain(2, List(node2)), Chain(3, List(node3)))

    val partitioning = List(bset1, bset2)

    val expected: Partitioning = List(bset1, bset2, Set(Chain(0, List(nodeConcurrent)))) //id

    new AGChainPartitioner().insert(nodeConcurrent, partitioning) shouldBe expected
  }

  it should "add a new chain in a BSet - insert into non-empty BSet(1)" in {
    val bset1 = Set(Chain(1, List(node2)))
    val bset2 = Set(Chain(2, List(node1)))

    val partitioning = List(bset1, bset2)

    new AGChainPartitioner().insert(nodeConcurrent, partitioning) shouldBe List(bset1, Set(Chain(2, List(node1)), Chain(0, List(nodeConcurrent))))
  }

  it should "add a new chain in a BSet - insert into non-empty BSet(2)" in {
    val bset1 = Set(Chain(1, List(node1)))
    val bset2 = Set(Chain(2, List(node2)), Chain(3, List(node3)))
    val bset3 = Set(Chain(4, List(node4)))

    val partitioning = List(bset1, bset2, bset3)

    new AGChainPartitioner().insert(nodeDependentTo4, partitioning) shouldBe List(bset1, bset2, Set(Chain(4, List(node4, nodeDependentTo4))))
  }

  it should "append to an existing chain in the first BSet correctly - only BSet(0) exists " in {
    val bset1 = Set(Chain(1, List(node3)))

    val partitioning = List(bset1)

    new AGChainPartitioner().insert(nodeDependentTo3, partitioning) shouldBe List(Set(Chain(1, List(node3, nodeDependentTo3))))
  }

  it should "append to an existing chain in the first BSet correctly - BSet(1) and BSet(2) exist" in {
    val bset1 = Set(Chain(1, List(node3)))
    val bset2 = Set(Chain(2, List(node4)))

    val partitioning = List(bset1, bset2)

    new AGChainPartitioner().insert(nodeDependentTo3, partitioning) shouldBe List(Set(Chain(1, List(node3, nodeDependentTo3))), bset2)
  }

  it should "append to an existing chain in an intra BSet correctly - BSet has a single chain" in {
    val bset1 = Set(Chain(1, List(node2)))
    val bset2 = Set(Chain(2, List(node3)))

    val partitioning = List(bset1, bset2)

    new AGChainPartitioner().insert(nodeDependentTo3, partitioning) shouldBe List(bset1, Set(Chain(2, List(node3, nodeDependentTo3))))
  }

  it should "append to an existing chain in an intra BSet correctly - BSet has two chains" in {
    val bset1 = Set(Chain(1, List(node2)))
    val bset2 = Set(Chain(2, List(node1)), Chain(3, List(node3)))

    val partitioning = List(bset1, bset2)

    new AGChainPartitioner().insert(nodeDependentTo3, partitioning) shouldBe List(bset1, Set(Chain(2, List(node1)), Chain(3, List(node3, nodeDependentTo3))))
  }

  it should "append to an existing chain in the last BSet correctly " in {
    val bset1 = Set(Chain(1, List(node3)))
    val bset2 = Set(Chain(2, List(node1)), Chain(3, List(node2)))
    val bset3 = Set(Chain(4, List(node4)), Chain(5, List(node5)))

    val partitioning = List(bset1, bset2, bset3)


    new AGChainPartitioner().insert(nodeDependentTo4, partitioning) shouldBe List(bset1, bset2, Set(Chain(4, List(node4, nodeDependentTo4)), Chain(5, List(node5))))
  }

  it should "append to an existing chain where it fits with more than one elements correctly - add into an intra BSet" in {
    val bset1 = Set(Chain(1, List(node3)))
    val bset2 = Set(Chain(2, List(node1)), Chain(3, List(node2)))
    val bset3 = Set(Chain(4, List(node4)), Chain(5, List(node5)))

    val partitioning = List(bset1, bset2, bset3)

    new AGChainPartitioner().insert(nodeDependentTo1And2, partitioning) shouldBe List(Set(Chain(3, List(node2))),
      Set(Chain(2, List(node1, nodeDependentTo1And2)), Chain(1, List(node3))), bset3)
  }

  it should "append to an existing chain where it fits with more than one elements correctly - add into the last BSet" in {
    val bset1 = Set(Chain(1, List(node3)))
    val bset2 = Set(Chain(2, List(node1)), Chain(3, List(node2)))
    val bset3 = Set(Chain(4, List(node4)), Chain(5, List(node5)))

    val partitioning = List(bset1, bset2, bset3)

    new AGChainPartitioner().insert(nodeDependentTo4And5, partitioning) shouldBe List(bset1, Set(Chain(5, List(node5))), Set(Chain(2, List(node1)),
      Chain(3, List(node2)), Chain(4, List(node4, nodeDependentTo4And5))))
  }
}
