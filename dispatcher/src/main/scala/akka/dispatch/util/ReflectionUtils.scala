package akka.dispatch.util

import java.lang.reflect.Method

import akka.actor.{Actor, ActorRef}
import akka.dispatch.Envelope

/**
  * Created by burcuozkan on 24/02/17.
  */
object ReflectionUtils {

  def createNewEnvelope(message: Object, sender: ActorRef): Envelope = {
    val testClass = classOf[Envelope]
    val ctor = testClass.getDeclaredConstructor(classOf[Any], classOf[ActorRef])
    ctor.setAccessible(true)
    val inst = ctor.newInstance(message, sender)
    println(inst)
    inst
  }

  def readPrivateVal(obj: Object, fieldName: String): Object = {
    val field = obj.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj)
  }

  def callPrivateMethod(x: AnyRef, methodName: String)(_args: Any*): Method = {
    val args = _args.map(_.asInstanceOf[AnyRef])

    def _parents: Stream[Class[_]] = Stream(x.getClass) #::: _parents.map(_.getSuperclass)

    val parents = _parents.takeWhile(_ != null).toList
    val methods = parents.flatMap(_.getDeclaredMethods)

    //methods.foreach(x => println(x.getName))

    val method = methods.find(_.getName == methodName).getOrElse(throw new IllegalArgumentException("Method " + methodName + " not found"))
    method.setAccessible(true)
    method.invoke(x, args: _*)
    method
  }

}
/**
class PrivateMethodCaller(x: AnyRef, methodName: String) {
  def apply(_args: Any*): Any = {
    val args = _args.map(_.asInstanceOf[AnyRef])
    def _parents: Stream[Class[_]] = Stream(x.getClass) #::: _parents.map(_.getSuperclass)
    val parents = _parents.takeWhile(_ != null).toList
    val methods = parents.flatMap(_.getDeclaredMethods)

    //methods.foreach(x => println(x.getName))

    val method = methods.find(_.getName == methodName).getOrElse(throw new IllegalArgumentException("Method " + methodName + " not found"))
    method.setAccessible(true)
    method.invoke(x, args : _*)
  }
}**/