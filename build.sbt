name := """pincette-xmlfilter"""
organization := "net.pincette"
version := "0.0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "net.pincette" % "pincette-common" % "1.3"
)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/.m2/repository")))
crossPaths := false
