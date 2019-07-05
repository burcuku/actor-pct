package explorer.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

abstract class ProtocolMessage

/** Message types used in the communication between the explorer and the dispatcher **/

case object Initiate extends ProtocolMessage  //may not be necessary

case object DispatchO extends ProtocolMessage

case object DispatchU extends ProtocolMessage

case class ReducePriority(event: Long, index: Int) extends ProtocolMessage

case object Terminate extends ProtocolMessage

case class Configuration(str: String) extends ProtocolMessage //todo fill in arguments in the Configuration case class
