import mill._
import mill.scalalib._
import publish._

object junitReport extends ScalaModule with PublishModule {

  override def scalaVersion = "2.13.10"

  override def publishVersion = "0.1.0"

  override def artifactName = "mill-junit-report-plugin"

  override def scalacOptions = T { super.scalacOptions() :+ "-deprecation" }

  def pomSettings = PomSettings(
    description = "Write test output to JUnit Report XML format",
    organization = "net.coacoas",
    url = "https://github.com/coacoas/mill-junit-report-plugin",
    licenses = Seq(License.`Apache-2.0`),
    versionControl =
      VersionControl.github("coacoas", "mill-junit-report-plugin"),
    developers = Seq(
      Developer("coacoas", "Bill Carlson", "https://github.com/coacoas"),
      Developer("vic", "Victor Borja", "https://github.com/vic")
    )
  )

  def ivyDeps = Agg(
    ivy"org.scala-lang.modules::scala-xml:2.1.0",
    ivy"com.lihaoyi::mill-scalalib:0.10.10"
  )

  object testMunit extends Tests with TestModule.Munit {
    def ivyDeps =
      Agg(
        ivy"org.scalameta::munit::0.7.29"
      )
  }

  object testZio extends Tests with TestModule {
    lazy val zioVersion = "2.0.5"

    def ivyDeps =
      Agg(
        ivy"dev.zio::zio-test:${zioVersion}",
        ivy"dev.zio::zio-test-sbt:${zioVersion}"
      )
    override def testFramework: T[String] = "zio.test.sbt.ZTestFramework"
  }
}
