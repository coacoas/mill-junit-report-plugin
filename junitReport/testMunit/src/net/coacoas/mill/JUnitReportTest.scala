package net.coacoas.mill

class JUnitReportTest extends munit.FunSuite {
  test("simple test") {
    munit.Assertions.assert(1 + 1 == 2, "Addition works")
  }

  test("second test, same as the first") {
    munit.Assertions.assert(1 + 1 == 2, "Addition works")
  }
}
