package net.coacoas.mill

import mill._
import mill.api.Ctx
import mill.define._
import mill.main.EvaluatorScopt
import mill.scalalib._
import os.Path
import upickle.default._

import scala.collection.immutable.AbstractSeq
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.xml._
import mainargs.ArgReader

object OtherReport extends ExternalModule {

  override def millDiscover: Discover[OtherReport.type] = Discover[OtherReport.type]

  def a() = T.command { println("Other report"); 2 }
}

object JUnitReport extends ExternalModule {
  implicit def millScoptEvaluatorReads[Evaluator] = new EvaluatorScopt()
  override def millDiscover: Discover[JUnitReport.type] = Discover[JUnitReport.type]

  /** Traverse all modules to find test modules. Note that `modules` contains
    * all modules within the build, not just immediate children.
    */
  def discoverTestModules(rootModule: Module): Seq[TestModule] =
    rootModule.millInternal.modules.collect { case tm: TestModule => tm }

  /** Given a sequence of paths, this will return the most recent by last
    * modified time, if any exist
    *
    * @param files
    * @return
    */
  def newestExistingFile(files: Seq[os.Path]): Option[os.Path] =
    files
      .filter(os.exists(_))
      .sortBy(p => os.stat(p).mtime.toMillis())
      .lastOption

  def findMillReports(
      module: TestModule,
      baseOutPath: os.Path
  ): Seq[os.Path] = {
    val moduleOutput =
      module.millModuleSegments.parts.foldLeft(baseOutPath)(_ / _)
    val testJson = moduleOutput / "test.dest" / "out.json"
    val testCachedJson = moduleOutput / "testCached.dest" / "out.json"

    newestExistingFile(Vector(testJson, testCachedJson)).toList
  }

  def generate(ev: mill.eval.Evaluator): Command[Unit] = T.command {
    val ctx: Ctx = implicitly
    for {
      testModule <- discoverTestModules(ev.rootModule)
      millReport <- findMillReports(testModule, ev.outPath)
    } yield {
      JUnitReportImpl.write(testModule, millReport, ctx.dest)
    }
    ()
  }
}

object JUnitReportImpl {
  def write(
      testModule: TestModule,
      millReport: os.Path,
      dest: os.Path
  ) = {
    val moduleName = testModule.millModuleShared.value.mkString(".")
    val result = generateJUnitReport(testModule, millReport)
    val node = JUnitReportImpl.resultXml(result, moduleName)
    val fileName = s"TEST-${moduleName}.xml"
    val writePath = (dest / fileName).toIO.getAbsolutePath()

    scala.xml.XML.save(
      filename = writePath,
      node = node,
      xmlDecl = true
    )

    node
  }


  def resultXml(
      results: Results,
      moduleName: String
  ): Node = {
    val suiteXml = results.suites.map { suite =>
      val testCases = suite.tests.map { test =>
        <testcase id={test.selector} classname={test.fullyQualifiedName} name={
          test.selector.substring(test.fullyQualifiedName.length + 1)
        } time={test.duration.toString}>
        {
          test.failure.map { failure =>
            <failure message={failure.message} type="ERROR">
ERROR: {failure.message}
Category: {failure.name}
File: {failure.trace(1).fileName}
Line: {failure.trace(1).lineNumber}
          </failure>
          }.orNull
        }
        {
          test.failure.map { failure =>
            <system-err>{
              failure.trace.mkString(
                s"${failure.name}: ${failure.message}",
                "\n    at ",
                ""
              )
            }</system-err>
          }.orNull
        }
        </testcase>
      }

      <testsuite id={suite.name} name={suite.name} tests={
        suite.tests.length.toString
      } failures={
        suite.tests.count(_.failure.isDefined).toString
      } time={suite.tests.map(_.duration).sum.toString}>
      {testCases}
      </testsuite>
    }

    val tests = results.suites.flatMap(_.tests)

    val node = <testsuites id={moduleName} name={moduleName}
    tests={
      tests.length.toString
    } failures={
      tests.count(_.failure.isDefined).toString
    } time={
      tests.map(_.duration).sum.toString
    }>{suiteXml}</testsuites>
    node
  }

  /** This generates the data from the out.json file built by mill.
    * Unfortunately, this file is built according to however the test framework
    * wants to build it, so it will not be consistent across multiple test
    * frameworks. It should be fairly consistent within a single test framework,
    * although it may not be ideal. For instance, munit reports
    * fullyQualifiedName and selector as the same values, while zio-test appends
    * the suite name to each test case.
    *
    * This simply tries to provide a reasonable default at this point.
    *
    * @param testModule
    * @param millReport
    * @return
    */
  def generateJUnitReport(
      testModule: TestModule,
      millReport: os.Path
  ): Results = {
    val json = ujson.read(millReport.toNIO)
    val testResults = json(1).value match {
      case tests: ArrayBuffer[_] =>
        tests.map { case test: ujson.Obj =>
          Test(
            fullyQualifiedName = test("fullyQualifiedName").str,
            selector = test("selector").str,
            duration = test("duration").num / 1000.0,
            failure = test("status").str match {
              case "Failure" =>
                Some(
                  Failure(
                    name = test("exceptionName")(0).str,
                    message = test("exceptionMsg")(0).str,
                    trace = test("exceptionTrace")(0).arr.map { st =>
                      val declaringClass = st("declaringClass").str
                      val methodName = st("methodName").str
                      val fileName = st("fileName")(0).str
                      val lineNumber = st("lineNumber").num.toInt
                      Trace(declaringClass, methodName, fileName, lineNumber)
                    }.toList
                  )
                )
              case _ => None
            }
          )
        }.toSeq
    }

    val suites: Seq[Suite] =
      testResults
        .groupBy(_.fullyQualifiedName)
        .map { case (suite, tests) => Suite(suite, tests) }
        .toSeq

    Results(suites)
  }

}

/*-------------------*/
/*--- Data Model ----*/
/*-------------------*/

case class Trace(
    declaringClass: String,
    methodName: String,
    fileName: String,
    lineNumber: Int
) {
  override def toString: String =
    s"${declaringClass}.${methodName}(${fileName}:${lineNumber})"
}

case class Failure(
    name: String,
    message: String,
    trace: Seq[Trace]
)

case class Test(
    fullyQualifiedName: String,
    selector: String,
    duration: Double,
    failure: Option[Failure]
)

case class MillReport(
    module: TestModule,
    outJson: os.Path
)

case class Suite(
    name: String,
    tests: Seq[Test]
)

case class Results(
    suites: Seq[Suite]
)
