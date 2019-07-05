import akka.actor.ActorRef


package object server {

  case class RegisterDispatcherServer(ref: ActorRef)

}
