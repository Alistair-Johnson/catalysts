package catalysts
package scalatest

import org.scalatest.{FunSuite, Matchers, PropSpec, Tag}
import org.scalatest.prop.{Configuration, PropertyChecks}
import org.scalactic.anyvals.{PosZDouble, PosInt}
import testkit.{TestModifier, TestSettings, TestSuite => BaseTestSuite}

trait ScalaTests extends BaseTestSuite with Configuration with Matchers with TestSettings with TestInstances {


  def checkConfiguration(mode:TestModifier): PropertyCheckConfiguration = {
    val (min,max) = checkSettings(mode)
    PropertyCheckConfiguration(PosInt.from(min).get, PosZDouble.from(max).get)
  }

  def notifyTestLoading(): Unit =
  {
    import scala.Console.GREEN

    if (shouldNotify)
      println(s"[info]${GREEN} ${testName} loading...")
  }

  implicit override val generatorDrivenConfig =  checkConfiguration(mod)
}

/**
 * An opinionated stack of traits to improve consistency and reduce
 * boilerplate in tests.
 */
trait TestSuite extends FunSuite with LawsChecks {
private var context: String = ""
object tagTest extends Tag("ScalaTests")

  def assertEquals[A](actual: => A, expected: => A): Unit = assert( actual == expected)
 def test(s: String)(a: => Any): Unit = test(s, tagTest){val r = a}

/*
  def shouldImpl[A](s: String, a: => Any): Unit = 
{/*
      val saved = context
      //println(s)
      context = s; try {val y = a} finally context = saved
  */
     describe(s){a}
    }

 def inImpl[A](s: String, a: => Any): Unit = it(s"$context should $s"){val x = a}
 */
}

trait TestProps extends PropSpec with PropertyChecks with ScalaTests

sealed trait TestInstances {
  import org.scalacheck.{Arbitrary, Gen}
  import org.scalacheck.Arbitrary.arbitrary
  import scala.util.{Failure, Success, Try}

  // To be replaced by https://github.com/rickynils/scalacheck/pull/170
  implicit def arbitraryTry[A: Arbitrary]: Arbitrary[Try[A]] =
    Arbitrary(Gen.oneOf(
      arbitrary[A].map(Success(_)),
      arbitrary[Throwable].map(Failure(_))))
}
