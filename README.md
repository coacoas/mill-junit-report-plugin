# THIS IS STILL A WORK IN PROGRESS

# Generate JUnit XML reports from Mill's test output.

Many CI/CD servers already support reading JUnit XML reports and integrate well with code-review and merge-requests.
However [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html) is still more of a Scala niche tool.

This repo defines a mill plugin that can generate [JUnit XML reports](https://www.ibm.com/docs/en/developer-for-zos/14.1.0?topic=formats-junit-xml-format), after `test` or `testCached` are executed.

## Usage:
```sh
mill --import ivy:net.coacoas::mill-junit-report-plugin:0.1.0 net.coacoas.mill.JUnitReport/generate
```
