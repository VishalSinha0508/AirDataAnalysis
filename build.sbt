ThisBuild / version := "0.1.0-SNAPSHOT"

name := "BI"

ThisBuild / scalaVersion := "2.13.10"

val versions = new {
  val spark = "3.3.2"
}
libraryDependencies ++= Seq(
   "org.apache.spark" %% "spark-core" % "3.2.2" ,
   "org.apache.spark" %% "spark-sql" % "3.2.2" ,

)
lazy val root = (project in file("."))
  .settings(
    name := "scalaPractice"
  )
