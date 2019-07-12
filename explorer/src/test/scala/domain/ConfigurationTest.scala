package domain

import explorer.protocol.{Event, MessageId, MessageSent, ProgramEvent}
import org.scalatest.{Assertions, FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer

class ConfigurationTest extends FlatSpec with Matchers {


  val root: Configuration = Configuration()

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


  it should "make the initial configuration with the supplies message and event ID" in {
    root.makeInit(0, "Init Message")
    assert(root.schedule().isDefined)
    assert(root.orderedEvents.size == 0)
    assert(!root.unorderedEvents.isEmpty)
  }

  it should "create correct children with new events" in  {


    val childConf: Configuration = new Configuration(root, eventList.toList, predecessors)

    assert(childConf.unorderedEvents.events.size == 2)
    assert(childConf.orderedEvents.size == 0)
    assert(childConf.executionGraph.nonEmpty)

  }


  it should "create correct children with different ordering of events" in {
    val childConf: Configuration = new Configuration(root, eventList.toList, predecessors)
    val subChild: Configuration = new Configuration(childConf, 0)

    for(i <- 0 to  subChild.orderedEvents.size) {
      val subChild2: Configuration = new Configuration(subChild, i)

      assert(subChild2.unorderedEvents.events.isEmpty)
      assert(subChild2.orderedEvents.size == 2)
      assert(subChild2.executionGraph == childConf.executionGraph)
      if(i==0)
        assert(subChild2.schedule().get == 1)
      else
        assert(subChild2.schedule().get==2)
      assert(!subChild2.isMaximal)
      assert(!subChild2.isFinal)
    }

  }

  it should "handle the case of final states correctly" in {
    val childConf: Configuration = new Configuration(root, List(), Map())

    assert(childConf.unorderedEvents.events.isEmpty)
    assert(childConf.isFinal)
  }



}
