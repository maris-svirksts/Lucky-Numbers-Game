name := "lucky-numbers-game"
version := "0.1"

scalaVersion := "2.13.6"
scalacOptions += "-deprecation"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % Test
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0"
libraryDependencies ++= Seq( 
  "com.typesafe.akka" %% "akka-http" % "10.5.2",
  "com.typesafe.akka" %% "akka-actor" % "2.8.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.2",
  "com.typesafe.akka" %% "akka-stream" % "2.8.3",
  "ch.qos.logback" % "logback-classic" % "1.3.9"
)
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.8.3" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.5.2" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.8.3" % Test
)