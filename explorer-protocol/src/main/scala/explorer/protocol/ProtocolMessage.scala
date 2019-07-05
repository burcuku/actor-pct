package explorer.protocol

abstract class ProtocolMessage

abstract class CommandRequest extends ProtocolMessage

abstract class CommandResponse extends ProtocolMessage

/** Message types used in the communication between the explorer and the dispatcher **/

case object Initiate extends CommandRequest  //may not be necessary

case object DispatchO extends CommandRequest

case object DispatchU extends CommandRequest

case class ReducePriority(event: Long, index: Int) extends CommandRequest

case object Terminate extends CommandRequest

case class Configuration(str: String) extends CommandResponse //todo fill in arguments in the Configuration case class
