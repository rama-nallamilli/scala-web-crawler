name := "web-crawler"

version := "0.1"

scalaVersion := "2.12.6"

mainClass in Compile := Some("com.crawler.Main")

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.11.3",
  "com.typesafe.play" %% "play-ahc-ws-standalone" %  "1.1.10",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test")
