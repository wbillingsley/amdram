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
    "org.scalameta" %%% "munit" % "0.7.26" % Test
  ),

  testFrameworks += new TestFramework("munit.Framework"),
)

lazy val aggregate = project.in(file("."))
  .aggregate(amdramJS, amdramJVM, jsuiJS, docsJS)
  .settings(commonSettings*)
  .settings(

    name := "amdram-aggregate",

    // Don't publish the root/aggregate project
    publish / skip := true
  )

lazy val amdram = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("amdram"))
  .settings(commonSettings:_*)
  .settings(
    name := "amdram",
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := false,

    Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val amdramJS = amdram.js
lazy val amdramJVM = amdram.jvm

/**
  * JavaScript UI for showing what's going on inside actors (TO-DO)
  */
lazy val jsui = crossProject(JSPlatform).crossType(CrossType.Pure).in(file("jsui"))
  .dependsOn(amdram)
  .settings(commonSettings:_*)
  .jsSettings(

    libraryDependencies ++= Seq(
      "com.wbillingsley" %%% "doctacular" % "0.3.0",
      "org.scala-js" %%% "scalajs-dom" % "2.3.0",
    ),

    name := "amdram-jsui",

    scalaJSUseMainModuleInitializer := false,
  )


lazy val jsuiJS = jsui.js

val deployFast = taskKey[Unit]("Copies the fastLinkJS script to compiled.js")
val deployFull = taskKey[Unit]("Copies the fullLinkJS script to compiled.js")

lazy val amdramDocs = crossProject(JSPlatform).crossType(CrossType.Pure).in(file("docs"))
  .dependsOn(jsui, amdram)
  .settings(commonSettings:_*)
  .settings(
    name := "amdram-docs",

    // Don't publish the documentation site to Maven Central
    publish / skip := true,

    scalacOptions ++= Seq("-unchecked", "-deprecation"),

    Compile / mainClass := Some("docs.Main"),
  )
  .jsSettings(
    // As we're doing concurrency, let's load a fair executor
    libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",

    scalaJSUseMainModuleInitializer := true,

    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }, // At the moment in Scala.js, ESModule would cause Closure minimisation to get turned off.

    deployFast := {
      val opt = (Compile / fastLinkJS).value
      (
        Process(s"npx webpack --config webpack.config.js --env entry=./.js/target/scala-3.1.2/amdram-docs-fastopt/main.js --env mode=development", Some(new java.io.File("docs")))
      ).!
    },

    deployFull := {
      val opt = (Compile / fullLinkJS).value
      (
        Process(s"npx webpack --config webpack.config.js --env entry=./.js/target/scala-3.1.2/amdram-docs-opt/main.js --env mode=production", Some(new java.io.File("docs")))
      ).!
    }
  )

lazy val docsJS = amdramDocs.js

//Uncomment this block to stop sbt loading but print out the dynamically generated version
//Global / onLoad := (Global / onLoad).value.andThen { s =>
//  dynverAssertTagVersion.value
//  s
//}

