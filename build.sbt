import sbt.Keys.libraryDependencies

scalaVersion := "3.6.4"
version := "0.1.0-SNAPSHOT"
organization := "qmf.poc.service"
organizationName := "qmf"

val zioVersion = "2.1.14"
val zioConfigVersion = "4.0.3"
val zioHttpVersion = "3.0.1"
val luceneVersion = "10.1.0"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-java-output-version", "8")

assemblyMergeStrategy in assembly := {
  case "module-info.class"                     => MergeStrategy.discard
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case x                                       => (assemblyMergeStrategy in assembly).value(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "service",
    assembly / mainClass := Some("qmf.poc.service.Main"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % "0.7.4",
      "org.apache.lucene" % "lucene-core" % luceneVersion,
      "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "org.scalameta" %% "munit" % "1.0.4" % Test
    )
  )
