package explorer.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

abstract class ProgramEvent

case class MessageSent(receiverId: String, senderId: String, msg: String) extends ProgramEvent

object ProgramEvents extends SprayJsonSupport with DefaultJsonProtocol {

  val MESSAGE_SENT = "MESSAGE_SENT"

  implicit object ProgramEventJsonFormat extends RootJsonFormat[ProgramEvent] {
    def write(event: ProgramEvent): JsObject = event match {
      case MessageSent(receiverId, senderId, msg) => JsObject(
        "eventType" -> JsString(MESSAGE_SENT),
        "receiverId" -> JsString(receiverId),
        "senderId" -> JsString(senderId),
        "msg" -> JsString(msg))
      case _ => serializationError("Program event cannot be read")
    }

    def read(json: JsValue): ProgramEvent = {
      val fields = json.asJsObject.fields
      fields("eventType") match {
        case JsString(ProgramEvents.MESSAGE_SENT) => MessageSent(fields("receiverId").convertTo[String], fields("senderId").convertTo[String],
          fields("msg").convertTo[String])
        case _ => deserializationError("Event expected")
      }
    }
  }
}


case class Event(id: MessageId, programEvent: ProgramEvent)

object Events extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object EventJsonFormat extends RootJsonFormat[Event] {
    def write(event: Event): JsObject = event match {
      case Event(id, pe) => JsObject("id" -> JsNumber(id), "pe" -> ProgramEvents.ProgramEventJsonFormat.write(pe))
      case _ => serializationError("Program event cannot be read")
    }

    def read(json: JsValue): Event = {
      val fields = json.asJsObject.fields
      Event(fields("id").convertTo[Long], ProgramEvents.ProgramEventJsonFormat.read(fields("pe")))
    }
  }
}



