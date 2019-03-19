package akka.dispatch.util

import akka.actor.ActorRef

object DispatcherUtils {
  val dispatcherConfigFile: String = "dispatcher.conf"

  private val systemActorPaths = Set("", "user", "system")
  private val pctActorNames = Set("DispatcherHelperActor", "PCTCPActor", "TaPCTCPActor",
    "POSActor", "DPOSActor", "RandomWalkActor", "RaposActor")

  /**
    * @param  actorRef an ActorRef
    * @return true if the actor is created and used by the ActorSystem
    */
  def isSystemActor(actorRef: ActorRef): Boolean = actorRef.path.elements match {
    case p :: Nil if systemActorPaths contains p => true
    case "user" :: p :: Nil if pctActorNames contains p => true
    case "system" :: _ => true
    case _ => false
  }
}
