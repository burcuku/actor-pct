package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class WaitingRoom(numChairs: Int) extends Actor with ActorLogging {

  private var waitingCustomers: List[ActorRef] = List()

  override def receive: Receive = {
    case GoesToWaitingRoom(customer) if waitingCustomers.size < numChairs =>
      log.info(customer + " directed to waiting room")
      waitingCustomers = waitingCustomers :+ customer
      customer ! InWaitingRoom

    case GoesToWaitingRoom(customer) =>
      customer ! WaitingRoomFull

    case BarberChairFree if waitingCustomers.nonEmpty =>
      log.info("Free barber chair")
      val customer = waitingCustomers.head
      waitingCustomers = waitingCustomers.tail
      customer ! GoToBarberChair
  }
}

object WaitingRoom {
  def props(numChairs: Int) = Props(new WaitingRoom(numChairs))
}
