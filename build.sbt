name := """cgq-streaming-greedy"""
organization := "com.drench"

version := "1.0-SNAPSHOT"

val greedy = taskKey[Unit]("Runs only the StreamingGreedy test")

lazy val root = (project in file(".")).enablePlugins(PlayScala)
  .settings(
    greedy := {
        val testResult = (Test / testOnly).toTask(" algorithm.StreamingGreedySpec").value
    }
  )

scalaVersion := "2.12.7"

exportJars := true

libraryDependencies += guice

// dependencyOverrides += "com.typesafe.play" %% "play-guice" % "2.6.5"
// libraryDependencies ++= Seq("com.typesafe.play" % "play-json_2.12" % "2.8.1" artifacts(Artifact("play-json_2.12", "jar", "jar")))

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2",
  "org.mockito" % "mockito-core" % "2.23.4"
)
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.11.46"
libraryDependencies += "net.debasishg" %% "redisclient" % "3.9"
// libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "2.5.6"
// libraryDependencies ++= Seq(caffeine)
// libraryDependencies ++= Seq(cacheApi)
libraryDependencies ++= Seq(ehcache)
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
)
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.3.1",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.1",
  "mysql" % "mysql-connector-java" % "6.0.6"
)
libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.drench.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.drench.binders._"

mappings in (Compile, packageBin) ++= {
  //(baseDirectory.value / "lib" / "play-json_2.12-2.6.10.jar") -> "lib/play-json_2.12-2.6.10.jar"
  //Path(baseDirectory.value).directory(file("lib"))
  Path.contentOf(file("lib"))
}

