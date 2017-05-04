package protocol

trait QueryRequest

case class ActionRequest(actionType: String, receiverId: String) extends QueryRequest

object QueryRequests {
  val ACTION_REQUEST = "ACTION_REQUEST"

  val ACTION_INIT = "__INIT__"
  val ACTION_END = "__END__"
  val ACTION_NEXT = "__NEXT__"
  val ACTION_DROP = "__DROP__"
}

