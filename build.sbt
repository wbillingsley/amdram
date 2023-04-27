import sbt.Keys.testFrameworks
import scala.sys.process._

val versionStr = "0.1-SNAPSHOT"

val scalaVersionStr = "3.1.2"

inThisBuild(List(
  organization := "com.wbillingsley",
  homepage := Some(url("https://www.wbillingsley.com/amdram")),
  licenses := List(License.MIT),
  developers := List(
    Developer(
      "wbillingsley",
      "William Billingsley",
      "wbillingsley@cantab.net",
      url("https://www.wbillingsley.com")
    )
  )
))

lazy val commonSettings = Seq(
  scalaVersion := scalaVersionStr,

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "2.3.0",
    "org.scalameta" %%% "munit" % "0.7.26" % Test
  ),

  testFrameworks += new TestFramework("munit.Framework"),

  Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
)

lazy val amdram = project.in(file("amdram"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings:_*)
  .settings(
    name := "amdram",
    scalaJSUseMainModuleInitializer := false,
  )


/**
  * JavaScript UI for showing what's going on inside actors (TO-DO)
  */
lazy val jsui = project.in(file("jsui"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(amdram)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies += "com.wbillingsley" %%% "doctacular" % "0.3.0",

    name := "amdram-jsui",
  )


val deployFast = taskKey[Unit]("Copies the fastLinkJS script to compiled.js")
val deployFull = taskKey[Unit]("Copies the fullLinkJS script to compiled.js")

lazy val docs = project.in(file("docs"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(amdram, jsui)
  .settings(commonSettings:_*)
  .settings(
    name := "amdram-docs",

    // Don't publish the documentation site to Maven Central
    publish / skip := true,
    
    scalaJSUseMainModuleInitializer := true,

    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }, // At the moment in Scala.js, ESModule would cause Closure minimisation to get turned off.

    scalacOptions ++= Seq("-unchecked", "-deprecation"),

    deployFast := {
      val opt = (Compile / fastLinkJS).value
      (
        Process(s"npx webpack --config webpack.config.js --env entry=./target/scala-3.1.2/amdram-docs-fastopt/main.js --env mode=development", Some(new java.io.File("docs")))
      ).!
    },

    deployFull := {
      val opt = (Compile / fullLinkJS).value
      (
        Process(s"npx webpack --config webpack.config.js --env entry=./target/scala-3.1.2/amdram-docs-opt/main.js --env mode=production", Some(new java.io.File("docs")))
      ).!
    }
  )

// Don't publish the root/aggregate project
publish / skip := true

//Uncomment this block to stop sbt loading but print out the dynamically generated version
//Global / onLoad := (Global / onLoad).value.andThen { s =>
//  dynverAssertTagVersion.value
//  s
//}

