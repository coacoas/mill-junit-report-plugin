package net.coacoas.mill

class OtherTest extends munit.FunSuite {
  test("simple other test") {
    munit.Assertions.assert(1 + 1 == 2, "Addition works")
  }

  test("second other test, same as the first") {
    munit.Assertions.assert(1 + 1 == 2, "Addition works")
  }

}
