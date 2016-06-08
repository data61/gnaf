package au.csiro.data61.gnaf.common.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Util {
  def loader = getClass.getClassLoader // or Thread.currentThread.getContextClassLoader

  /** Get a Scala singleton Object.
    * @param fqn object's fully qualified name
    * @return object as type T
    */
  def getObject[T](fqn: String): T = {
    val m = scala.reflect.runtime.universe.runtimeMirror(loader)
    m.reflectModule(m.staticModule(fqn)).instance.asInstanceOf[T]
  }

  /**
   * It appears that configuring a logger name containing a '$' in logback.xml doesn't work, so convert Scala object names ending in '$' to use '.' instead.
   */
  def logName(c: Class[_]) = c.getName.replace('$', '.')
  
  def getLogger(c: Class[_]) = Logger(LoggerFactory.getLogger(logName(c)))
}
