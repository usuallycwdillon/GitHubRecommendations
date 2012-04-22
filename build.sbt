name := "GitHub Recommendations"

version := "0.0.1"

scalaVersion := "2.9.1"

// Serialization doesn't work without forking in SBT. I don't know why.
fork in run := true

// This is going to be a problem on my MacBook Air.
javaOptions in run += "-Xmx10G"

// Godsend.
javaOptions in run += "-XX:+UseCompressedOops"

// Much nicer than Java time.
libraryDependencies += "joda-time" % "joda-time" % "2.0" 

// Joda time will bitch without theses string conversion routines.
libraryDependencies += "org.joda" % "joda-convert" % "1.1"

// Makes Joda nicer.
libraryDependencies += "org.scala-tools.time" % "time_2.9.1" % "0.5"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "1.7",
  "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "1.2.1-SNAPSHOT"
)


resolvers ++= Seq(
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases"  at "http://scala-tools.org/repo-releases",
  "sonatype.rep" at "https://oss.sonatype.org/content/repositories/public/"
)

scalacOptions += "-deprecation"


