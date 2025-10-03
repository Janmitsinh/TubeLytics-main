name := """play-java-seed"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)



scalaVersion := "2.13.0"


libraryDependencies += guice
lazy val akkaVersion = "2.6.14"
lazy val akkaHttpVersion = "10.1.14"

enablePlugins(JacocoPlugin)

libraryDependencies += guice
libraryDependencies ++= Seq(
  // Other dependencies
  "com.google.apis" % "google-api-services-youtube" % "v3-rev222-1.25.0", // YouTube API
  "com.google.api-client" % "google-api-client" % "1.32.1",               // Google API client
  "com.google.http-client" % "google-http-client-jackson2" % "1.39.2",


  // Testing dependencies

  // JaCoCo (for code coverage)
)
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
libraryDependencies += "com.typesafe.play" %% "play-cache" % "2.8.8"
libraryDependencies ++= Seq(
  // JUnit 5
  "org.junit.jupiter" % "junit-jupiter-api" % "5.8.2" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.8.2" % Test,

  // Mockito for mocking
  "org.mockito" % "mockito-core" % "5.1.1" % Test,
  "org.mockito" % "mockito-scala_2.13" % "1.9.0" % Test, // For Scala projects

  // JUnit-5 integration with Play (optional if needed)
  "org.mockito" % "mockito-junit-jupiter" % "5.1.1" % Test
)
libraryDependencies += "com.google.guava" % "guava" % "30.1-jre"

libraryDependencies += "org.mockito" % "mockito-inline" % "5.2.0"
testFrameworks += new TestFramework("org.junit.platform.console.ConsoleLauncher")


PlayKeys.devSettings += "play.server.http.idleTimeout" -> "infinite"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.6.14" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.mockito" % "mockito-core" % "4.11.0" % Test,
)


