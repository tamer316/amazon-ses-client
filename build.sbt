name := "amazon-ses-client"
organization := "dev.tamer"
version := "2.1.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  scalastyleFailOnError := true
)

val unitSuffixes = List("Spec", "Test")
val integrationSuffixes = unitSuffixes.map("Integration" + _)

def integrationTestFilter(name: String): Boolean = integrationSuffixes.exists(name.endsWith)
def unitTestFilter(name: String): Boolean =
  unitSuffixes.exists(s => name.endsWith(s) && !integrationTestFilter(name))

lazy val IntTest = config("it") extend Test

lazy val root = (project in file("."))
  .configs(IntTest)
  .settings(commonSettings: _*)
  .settings(inConfig(IntTest)(Defaults.testTasks): _*)
  .settings(
    testOptions in Test := Seq(Tests.Filter(unitTestFilter)),
    testOptions in IntTest := Seq(Tests.Filter(integrationTestFilter))
  )
