package domain

import explorer.protocol.{MessageId, ProgramEvent}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer






case class Configuration () {


  type EventId = String //todo:  change this to whatever is unique

  var executionGraph: Map[EventId, Set[EventId]] = Map() //immutable by default


  var enabledDep : Map[EventId, Set[EventId]] = Map() // opt: Make this linear(1)

  var labelMap : Map[EventId, MessageId] = Map() //opt: Make this linear(2)

  var isMaximal: Boolean = false
  var isFinal: Boolean = false

  class UnorderedEvents(val events: mutable.Queue[EventId] = mutable.Queue()) {

    def addEvent(newEvent: EventId): Unit = {
        events.enqueue(newEvent)
    }

    def getEvent: EventId = {
       events.head
    }

    def popEvent: EventId = {
      assert(events.nonEmpty, "Trying to pop an empty unordered event")
      events.dequeue()
    }

    def isEmpty: Boolean = {
      events.isEmpty
    }

    def getClone: mutable.Queue[EventId] = {
      events.clone()
    }


  }

  class OrderedEvents(val events : ListBuffer[EventId] = ListBuffer()) {

    def addEvent(eventId: EventId, pos: Int): Unit = {
      events.insert(pos, eventId)
    }

    def removeHead: EventId = {
      assert(events.nonEmpty, "Trying to pop an empty Ordered Event")
      events.remove(0)
    }

    def size: Int = {
      events.size
    }

    def getClone: ListBuffer[EventId] = {
      events.clone()
    }

    def getEvent: EventId = events.head
  }

  var unorderedEvents: UnorderedEvents = new UnorderedEvents()

  var orderedEvents: OrderedEvents = new OrderedEvents()

//  var tempCounter: Int = 0
  private def getEventId(e: ProgramEvent): EventId = {
    //TODO
//    tempCounter+=1
//    tempCounter

    e.toString
  }

  private def getNextEvent : Option[EventId] = {
    if(!unorderedEvents.isEmpty)
      Option[EventId](this.unorderedEvents.getEvent)
    else if(orderedEvents.size!=0)
      Option[EventId](orderedEvents.getEvent)
    else
      Option.empty[EventId]
  }


  private[this] def init(parent: Configuration):Unit = {
    this.unorderedEvents = new UnorderedEvents(parent.unorderedEvents.getClone)
    this.orderedEvents = new OrderedEvents(parent.orderedEvents.getClone)
    this.executionGraph  = parent.executionGraph
    this.labelMap = parent.labelMap
  }

  def this(parent: Configuration, pos:Int) {
    this()
    init(parent)
    orderedEvents.addEvent(unorderedEvents.popEvent, pos)
    enabledDep = parent.enabledDep
  }

  def popSched(): Unit = {
    if(!unorderedEvents.isEmpty)
      unorderedEvents.popEvent
    else {
      orderedEvents.removeHead
    }
  }

  def this(parent: Configuration,  events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) {
    this()
    init(parent)

    val eventSched: Option[EventId] = parent.getNextEvent
    eventSched match {
      case Some(eventId) =>
        executionGraph += (eventId -> parent.enabledDep(eventId))
        enabledDep = parent.enabledDep
        enabledDep-=eventId
        popSched()
      case None =>
        println("Dummy Initial Parent")
    }

    events.foreach(e => {
      val eventIdT: EventId = getEventId(e._2)
      labelMap += (eventIdT -> e._1)

      var predec: Set[EventId] = Set[EventId]()
      predecessors(e._1).foreach(d => predec += labelMap.find(_._2 == d).get._1)

      enabledDep += (eventIdT -> predec)
      unorderedEvents.addEvent(eventIdT)
    }
    )

    isMaximal = true
    isFinal = unorderedEvents.isEmpty && (orderedEvents.size==0)

  }

  def schedule(): Option[MessageId] = {
    getNextEvent match  {
      case Some(id) =>
        Option[MessageId](this.labelMap(id))
      case None =>
        Option.empty[MessageId]
    }

  }

  def makeInit(messageId: MessageId, eventId: EventId): Unit = {
    isFinal = false
    isMaximal = true
    unorderedEvents = new UnorderedEvents(mutable.Queue(eventId))
    labelMap += (eventId->messageId)
    enabledDep+=(eventId->Set())
  }


  def getChildren(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]], depth: Int) : List[Configuration] = {

    if(isFinal)
      return List[Configuration]()

    val children: ListBuffer[Configuration] = new ListBuffer[Configuration]()

    children.append(new Configuration(this, events, predecessors))

    if (!unorderedEvents.isEmpty && orderedEvents.size < depth) {
      for(i <- 0 to orderedEvents.size) {
        children.prepend(new Configuration(this, i))
      }
    }

    children.toList
  }

  override def toString: String = {
    var s: String = ""
    s+="\nexecGraph: "
    s+= executionGraph.toString()
    s+="\nunorderedEvents"
    s+=unorderedEvents.events.toString()
    s+="\norderedEvents"
    s+=orderedEvents.events.toString()
    s+="\nlabelMap:: "
    s+=labelMap.toString()
    s
  }
}
