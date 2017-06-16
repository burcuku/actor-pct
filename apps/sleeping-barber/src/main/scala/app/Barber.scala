package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class Barber extends Actor with ActorLogging {

  def sleeping: Receive = {
    case CutHair(customer) =>
      log.info("Barber - Started cutting hair of " + customer.path.name)
      context.become(cuttingHair(customer))
      self ! ProcessingDone

    case _ => log.info("Barber - Received unknown message")
  }

  def cuttingHair(customer: ActorRef): Receive = {
    case ProcessingDone =>
      log.info("Barber - Finished cutting hair of " + customer.path.name)
      customer ! HairCutDone
      context.become(sleeping)

    case _ => log.info("Barber - Received unknown message")
  }

  def receive = sleeping
}

object Barber {
  def props = Props(new Barber())
}