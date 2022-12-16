package net.coacoas.mill

import zio.test._
import zio.test.Assertion._

object JUnitReportZIOTest extends ZIOSpecDefault {
  def spec = suite("JUnitSpec") {
    test("simple test") {
      assertTrue(1 + 1 == 2)
    } +
    test("second test, same as the first") {
      assertTrue(1 + 1 == 2)
    }
  }
}
