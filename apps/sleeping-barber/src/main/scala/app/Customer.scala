package app

import akka.actor.{Actor, ActorLogging, Props}

class Customer(customerName: String) extends Actor with ActorLogging {

  def receive: Receive = {
    case EntersShop =>
      log.info(customerName + " - Enters shop")
      BarberShop.barberChair ! WantsBarberChair(self)
      context.become(checkingBarberChair)

    case _ => log.info(" - Received unknown message ")
  }

  def checkingBarberChair: Receive = {
    case OnBarberChair =>
      log.info(customerName + " - On barber chair")
      context.become(onBarberChair)

    case BarberChairOccupied =>
      log.info(customerName + " - Goes to the waiting room")
      BarberShop.waitingRoom ! GoesToWaitingRoom(self)
      context.become(waiting)

    case HairCutDone =>
      log.error("Hair cut while the customer is not on the barber chair")
      throw new Exception("Hair cut while the customer is not on the barber chair") // hits the bug!

    case _ => log.info("Customer - Received unknown message")
  }

  def onBarberChair: Receive = {
    case HairCutDone =>
      log.info(customerName + " - Exiting shop")
      BarberShop.barberChair ! FreesBarberChair(self)

    case _ => log.info("Customer - Received unknown message")
  }

  def waiting: Receive = {
    case InWaitingRoom =>
      log.info(customerName + " - is in the waiting room")

    case WaitingRoomFull =>
      log.info(customerName + " - Waiting room full, Exiting shop")

    case GoToBarberChair =>
      log.info(customerName + " - Goes to the barber chair")
      BarberShop.barberChair ! WantsBarberChair(self)
      context.become(checkingBarberChair)

    case _ => log.info("Customer - Received unknown message")
  }
}

object Customer {
  def props(name: String) = Props(new Customer(name))
}