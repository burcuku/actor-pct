package akka.dispatch.util

import java.lang.reflect.Method

import akka.actor.{Actor, ActorRef}
import akka.dispatch.Envelope

object ReflectionUtils {

  def readPrivateVal(obj: Object, fieldName: String): Object = {
    val field = obj.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj)
  }

  def callPrivateMethod(x: AnyRef, methodName: String)(_args: Any*): AnyRef = {
    val args = _args.map(_.asInstanceOf[AnyRef])

    def _parents: Stream[Class[_]] = Stream(x.getClass) #::: _parents.map(_.getSuperclass)

    val parents = _parents.takeWhile(_ != null).toList
    val methods = parents.flatMap(_.getDeclaredMethods)

    val method = methods.find(_.getName == methodName).getOrElse(throw new IllegalArgumentException("Method " + methodName + " not found"))
    method.setAccessible(true)
    val res = method.invoke(x, args: _*)
    res
  }

  def createNewEnvelope(message: Object, sender: ActorRef): Envelope = {
    val testClass = classOf[Envelope]
    val ctor = testClass.getDeclaredConstructor(classOf[Any], classOf[ActorRef])
    ctor.setAccessible(true)
    ctor.newInstance(message, sender)
  }
}
