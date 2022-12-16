package net.coacoas.mill

import zio.test._
import zio.test.Assertion._

object OtherZIOTest extends ZIOSpecDefault {
  def spec = suite("Other Spec") {
    test("yet another simple test") {
      assertTrue(1 + 1 == 2)
    } +
    test("and even another second test, same as the first") {
      assertTrue(1 + 1 == 2)
    }
  }
}
