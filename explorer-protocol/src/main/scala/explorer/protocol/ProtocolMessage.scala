package explorer.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import explorer.protocol.Events.EventJsonFormat
import spray.json._

abstract class Request
case class DispatchMessageRequest(msgId: MessageId) extends Request
case object InitRequest extends Request
case object TerminateRequest extends Request

abstract class Response
case class CommandResponse(events: List[Event], predecessors: Map[MessageId, Set[MessageId]]) extends Response

case class NewEvents(events: List[(MessageId, ProgramEvent)], predecessors: Map[MessageId, Set[MessageId]]) extends Response //todo merge + adapter from the dispatcher

object Responses extends SprayJsonSupport with DefaultJsonProtocol {
  val ADDED_EVENTS_RESPONSE = "ADDED_EVENTS_RESPONSE"

  implicit object ResponseJsonFormat extends RootJsonFormat[Response] {

    def write(c: Response): JsObject = c match {
      case CommandResponse(events: List[Event], predecessors: Map[MessageId, Set[MessageId]]) => JsObject(
        "events" -> JsArray(events.map(e => Events.EventJsonFormat.write(e)))
        //,"predecessors" -> predecessors.toJson //todo
      )
      case _ => serializationError("Command Response cannot be read")
    }

    def read(json: JsValue): CommandResponse = {
      val fields = json.asJsObject.fields
      fields("responseType") match {
        case JsString(Responses.ADDED_EVENTS_RESPONSE) => {
          val events = fields("events").asInstanceOf[JsArray].elements.map(EventJsonFormat.read).toList
          CommandResponse(events,
            //  fields("predecessors").asInstanceOf[JSMap]
            Map() //todo
          )
        }
        case _ => deserializationError("Command Response expected")
      }
    }
  }
}

//todo fill in with other requests
object Requests extends SprayJsonSupport with DefaultJsonProtocol {

  val DISPATCH_MESSAGE_REQUEST = "DISPATCH_MESSAGE_REQUEST"
  val INIT_REQUEST = "INIT_REQUEST"
  val TERMINATE_REQUEST = "TERMINATE_REQUEST"

  implicit object RequestJsonFormat extends RootJsonFormat[Request] {

    def write(c: Request): JsObject = c match {
      case InitRequest => JsObject("requestType" -> JsString(INIT_REQUEST))
      case TerminateRequest => JsObject("requestType" -> JsString(TERMINATE_REQUEST))
      case DispatchMessageRequest(messageId: MessageId) => JsObject(
        "requestType" -> JsString(DISPATCH_MESSAGE_REQUEST),
        "messageId" -> JsNumber(messageId))
      case _ => serializationError("Command Response cannot be read")
    }

    def read(json: JsValue): Request = {
      val fields = json.asJsObject.fields
      fields("requestType") match {
        case JsString(Requests.DISPATCH_MESSAGE_REQUEST) => DispatchMessageRequest(
          fields("messageId").asInstanceOf[JsNumber].convertTo[Long])
        case JsString(Requests.INIT_REQUEST) => InitRequest
        case JsString(Requests.TERMINATE_REQUEST) => TerminateRequest
        case _ => deserializationError("Command Response expected")
      }
    }
  }
}