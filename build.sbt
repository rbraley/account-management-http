import Settings._

// Global Settings
ThisBuild / scalaVersion    := "2.13.8"
ThisBuild / organization    := "rbraley"
ThisBuild / versionScheme   := Some("early-semver")
ThisBuild / dynverSeparator := "-"
ThisBuild / conflictManager := ConflictManager.latestRevision
ThisBuild / javacOptions ++= Seq("-source", "17", "-target", "17")
ThisBuild / scalacOptions ++= Seq("-Ymacro-annotations", "-target:jvm-17")

lazy val root = (project in file("."))
  .settings(
    name         := "account-management-http",
    publish      := {},
    publishLocal := {}
  )
  .settings(CommandAliases.aliases)
  .aggregate(commons, `shard-manager`, `account-management-http`)

lazy val commons = project
  .settings(
    name         := "commons",
    publish      := {},
    publishLocal := {}
  )
  .settings(commonSettings, scalafixSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Zio.all,
      Dependencies.Shardcake.all
    ).flatten
  )
  .enablePlugins(ScalafixPlugin)

val commonsDep = commons % "compile->compile;test->test"

lazy val `shard-manager` = project
  .settings(
    name := "shard-manager"
  )
  .settings(commonSettings, scalafixSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Zio.all,
      Dependencies.Shardcake.all
    ).flatten
  )
  .enablePlugins(ScalafixPlugin)
  .dependsOn(commonsDep)
  .settings(
    dockerSettings,
    Docker / packageName        := "shard-manager",
    Docker / dockerExposedPorts := Seq(54321, 8080)
  )
  .enablePlugins(DockerPlugin, AshScriptPlugin)

lazy val `account-management-http` = project
  .settings(
    name := "account-management-http"
  )
  .settings(commonSettings, scalafixSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.Zio.all,
      Dependencies.Shardcake.all
    ).flatten
  )
  .enablePlugins(ScalafixPlugin)
  .dependsOn(commonsDep)
  .settings(
    Universal / mappings += {
      val conf = (Compile / resourceDirectory).value / "application.conf"
      conf -> "application.conf"
    },
    dockerSettings,
    Docker / packageName        := "account-management-http",
    Docker / dockerExposedPorts := Seq(54321)
  )
  .enablePlugins(DockerPlugin, AshScriptPlugin)
