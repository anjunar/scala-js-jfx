import org.scalajs.linker.interface.{ESVersion, ModuleKind}
import org.scalajs.sbtplugin.ScalaJSPlugin

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.2"

lazy val commonJsSettings = Seq(
  scalaJSLinkerConfig ~= (
    _.withModuleKind(ModuleKind.ESModule)
      .withESFeatures(_.withESVersion(ESVersion.ES2021))
    )
)

lazy val jfx = (project in file("jfx"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "scala-js-jfx",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.1",
    libraryDependencies += ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13)
  )
  .settings(commonJsSettings)

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(jfx)
  .settings(
    name := "scala-js-jfx-app",
    scalaJSUseMainModuleInitializer := true
  )
  .settings(commonJsSettings)

lazy val root = (project in file("."))
  .aggregate(jfx, app)
  .settings(
    publish / skip := true
  )