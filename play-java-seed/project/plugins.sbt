// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.8")

// Defines scaffolding (found under .g8 folder)
// http://www.foundweekends.org/giter8/scaffolding.html
// sbt "g8Scaffold form"
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.11.0")

addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.3.0")

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("releases"),
  "SBT Plugin Releases" at "https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"
)




