package au.csiro.data61.gnaf.test

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import Main._

class MainTest extends FlatSpec with Matchers {
  
  val s = "some test string"
  
  val typo = "\\S{2}~".r

  "mkTypo" should "make one random typo and not in the first two chars of a word" in {
    val s = Seq(Some("the quick brown fox"), None, Some("jumped over the lazy"), Some("fence"))
    (0 to 100).foreach { _ =>
      (s zip mkTypo(s)).count { case (a, b) =>
        val notEq = a != b
        if (notEq) {
          log.debug(b.toString)
          b.isDefined should be (true)
          typo.findFirstIn(b.get).isDefined should be (true)
        }
        notEq
      } should be(1)
    }
  }
  
}