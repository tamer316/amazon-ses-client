libraryDependencies ++= {
  val logbackV = "1.1.2"
  val grizzledV = "1.3.0"
  val awsSesV = "1.11.87"
  val mailV = "1.5.2"
  val scalaTestV = "3.0.1"
  val specs2V = "3.8.8"
  Seq(
    "ch.qos.logback" % "logback-classic" % logbackV,
    "org.clapper" %% "grizzled-slf4j" % grizzledV,
    "com.amazonaws" % "aws-java-sdk-ses" % awsSesV,
    "com.sun.mail" % "javax.mail" % mailV,
    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "org.specs2" % "specs2-core_2.11" % specs2V % "test",
    "org.specs2" % "specs2-mock_2.11" % specs2V % "test"
  )
}

Revolver.settings