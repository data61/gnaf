package au.com.data61.gnaf.util

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

object Logging {
  
  /**
   * It appears that configuring a logger name containing a '$' in logback.xml doesn't work, so convert Scala object names ending in '$' to use '.' instead.
   */
  def logName(c: Class[_]) = c.getName.replace('$', '.')
  
  def getLogger(c: Class[_]) = Logger(LoggerFactory.getLogger(logName(c)))
}