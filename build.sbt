import sbt.Keys.libraryDependencies

scalaVersion := "3.6.2"
version := "0.1.0-SNAPSHOT"
organization := "qmf.poc.service"
organizationName := "qmf"

val zioVersion = "2.1.14"
val zioConfigVersion = "4.0.3"
val zioHttpVersion = "3.0.1"
val luceneVersion = "10.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "service",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "org.apache.lucene" % "lucene-core" % luceneVersion,
      "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "org.scalameta" %% "munit" % "1.0.4" % Test
    ),
  )
