package domain

import akka.dispatch.ProgramEvent
import explorer.protocol.Configuration
import protocol.MessageId

import scala.collection.mutable
import scala.collection.mutable.ListBuffer






case class Configuration () {


  type EventId = Int //todo:  change this to whatever is unique

  var executionGraph: Map[EventId, Set[EventId]] = Map() //immutable by default


  var enabledDep : Map[EventId, Set[EventId]] = Map() // opt: Make this linear(1)

  var labelMap : Map[EventId, MessageId] = Map() //opt: Make this linear(2)

  var isMaximal: Boolean = false
  var isFinal: Boolean = false

  case class UnorderedEvents() {

    private var events: mutable.Queue[EventId] = mutable.Queue() //mutable by definition

    def this(events: mutable.Queue[EventId]) = {
      this()
      this.events = events
    }

    def addEvent(newEvent: EventId): Unit = {
        events.enqueue(newEvent)
    }

    def getEvent: EventId = {
       events.head
    }

    def popEvent: EventId = {
      events.dequeue()
    }

    def isEmpty: Boolean = {
      events.isEmpty
    }

    def getClone: mutable.Queue[EventId] = {
      events.clone()
    }


  }

  case class OrderedEvents() {

    private var events: ListBuffer[EventId] = ListBuffer() //mutable by definition


    def this(events: ListBuffer[EventId]) {
      this()
      this.events = events
    }

    def addEvent(eventId: EventId, pos: Int): Unit = {
      events.insert(pos, eventId)
    }

    def removeHead: EventId = {
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

  var unorderedEvents: UnorderedEvents =  UnorderedEvents()

  var orderedEvents: OrderedEvents = OrderedEvents()

  private def getEventId(e: ProgramEvent): EventId = {
    0
  }

  private[this] def init(parent: Configuration):Unit = {
    this.orderedEvents = parent.orderedEvents
    this.unorderedEvents = new UnorderedEvents(parent.unorderedEvents.getClone)
    this.orderedEvents = new OrderedEvents(parent.orderedEvents.getClone)
    this.executionGraph  = parent.executionGraph
    this.labelMap = parent.labelMap
  }

  def this(parent: Configuration, pos:Int) {
    this()
    this.init(parent)
    this.orderedEvents.addEvent(this.unorderedEvents.popEvent, pos)
    this.enabledDep = parent.enabledDep
  }

  def this(parent: Configuration,  events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) {
    this()
    this.init(parent)

    val eventId: EventId = parent.unorderedEvents.getEvent
    this.executionGraph += (eventId -> parent.enabledDep(eventId))

    this.enabledDep = parent.enabledDep
    this.enabledDep-=eventId
    for(e <- events) {
      val eventIdT: EventId = getEventId(e._2)
      this.labelMap +=(eventIdT -> e._1)

      val predec: mutable.Set[EventId] = mutable.Set[EventId]()

      for(d<-predecessors(e._1)) {
        predec.add(this.labelMap.find(_._2 == d).get._1)
      }

      this.enabledDep+=(eventIdT -> Set(predec))
      this.unorderedEvents.addEvent(eventIdT)
    }

    if(!this.unorderedEvents.isEmpty)
      this.unorderedEvents.popEvent
    else
      this.orderedEvents.removeHead

    this.isMaximal = true
    this.isFinal = this.unorderedEvents.isEmpty && (this.orderedEvents.size==0)

  }

  def schedule(): Option[MessageId] = {

    if(!this.unorderedEvents.isEmpty)
      Option[MessageId](this.labelMap(this.unorderedEvents.getEvent))
    else if(this.orderedEvents.size!=0)
      Option[MessageId](this.labelMap(this.orderedEvents.getEvent))
    else
      Option.empty[MessageId]
  }


}
