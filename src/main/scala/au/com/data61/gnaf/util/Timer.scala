package au.com.data61.gnaf.util

/** Accumulates time since constructed or reset.
 */
class Timer {
  private var t0 = 0L      // start of currently measured time period
  private var elapsed = 0L // sum of previous time periods ended by stop/elapsedSecs

  reset

  def reset = {
    elapsed = 0L
    start
  }

  /** `start` and `stop` need not be used - used to discard (not accumulate) the time between `stop` and `start`. */
  def start = t0 = System.currentTimeMillis

  def stop = {
    val t = System.currentTimeMillis
    elapsed += (t - t0)
    t0 = t // so subsequent `start` isn't required 
  }

  /** Get accumulated seconds.
   * 
   *  Also does `stop`, so time between `elapsedSecs` and a subsequent `start` would not be accumulated.
   */
  def elapsedSecs: Float = {
    stop
    elapsed * 1e-3f
  }

}

object Timer {
  
  private lazy val log = Logging.getLogger(getClass)
  
  def apply() = new Timer()
  
  /** Log elapsed time as info.
   *  
   *  Usage:
   *  {{{
   *  val a: A = timed("it took {} secs") {
   *     ...
   *     new A()
   *  }
   *  }}}
   *  
   *  @param msg contains "{}" which is replaced by the elapsed time in secs
   *  @param action thunk to execute and time
   */
  def timed[T](msg: String)(action: => T) = {
    val t = Timer()
    val x = action
    log.info(msg, t.elapsedSecs.toString)
    x
  }
}