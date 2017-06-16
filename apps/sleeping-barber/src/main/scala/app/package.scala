import akka.actor.ActorRef

package object app {

  case class GoToBarberChair(customer: ActorRef)
  case class WantsBarberChair(customer: ActorRef)
  case class FreesBarberChair(customer: ActorRef)
  case object OnBarberChair
  case object BarberChairOccupied

  case class CutHair(customer: ActorRef)
  case object ProcessingDone
  case object HairCutDone
  case object BarberChairFree

  case object InWaitingRoom
  case class GoesToWaitingRoom(customer: ActorRef)
  case object WaitingRoomFull
  case object EntersShop
  case object ExitsShop
}
