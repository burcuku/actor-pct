import akka.actor.ActorRef


package object server {

  case class RegisterExplorerServer(ref: ActorRef)

}
