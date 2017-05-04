package akka.dispatch.util

/**
  * Created by burcuozkan on 27/02/17.
  */
object FunUtils {
  def toRunnable(fun: () => Unit): Runnable = new Runnable() { def run(): Unit = fun() }
}
