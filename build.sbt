import Base._
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
import ScoverageSbtPlugin._

/**
 * These aliases serialise the build for the benefit of Travis-CI, also useful for pre-PR testing.
 * If new projects are added to the build, these must be updated.
 */
addCommandAlias("buildJVM", ";macrosJVM/compile;platformJVM/compile;scalatestJVM/test;specs2/test;testkitJVM/compile;testsJVM/test")
addCommandAlias("validateJVM", ";scalastyle;buildJVM")
addCommandAlias("validateJS", ";macrosJS/compile;platformJS/compile;scalatestJS/test;testkitJS/compile;testsJS/test")
addCommandAlias("validate", ";validateJS;validateJVM")
addCommandAlias("validateAll", s";++$scalacVersion;+clean;+validate;++$scalacVersion;docs/makeSite") 

/**
 * Build settings
 */
val home = "https://github.com/InTheNow/scala-bricks"
val repo = "git@github.com:InTheNow/scala-bricks.git"
val api = "https://InTheNow.github.io/scala-bricks/api/"
val license = ("Apache License", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

val disciplineVersion = "0.4"
val macroCompatVersion = "1.0.0"
val paradiseVersion = "2.1.0-M5"
val scalacheckVersion = "1.12.4"
val scalatestVersion = "3.0.0-M7"
val scalacVersion = "2.11.7"
val specs2Version = "3.6.4"

lazy val buildSettings = Seq(
  organization := "org.typelevel",
  scalaVersion := scalacVersion,
  crossScalaVersions := Seq("2.10.5", scalacVersion)
)

/**
 * Common settings
 */
lazy val commonSettings = sharedCommonSettings ++ Seq(
  scalacOptions ++= commonScalacOptions,
  parallelExecution in Test := false
  // resolvers += Resolver.sonatypeRepo("snapshots")
) ++ warnUnusedImport ++ unidocCommonSettings

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage
)

lazy val commonJvmSettings = Seq(
 // testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF)
)

/**
 * Bricks - This is the root project that aggregates the bricksJVM and bricksJS sub projects
 */
lazy val bricksSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val bricks = project.in(file("."))
  .settings(moduleName := "root")
  .settings(bricksSettings)
  .settings(noPublishSettings)
  .settings(console <<= console in (bricksJVM, Compile))
  .aggregate(bricksJVM, bricksJS)
  .dependsOn(bricksJVM, bricksJS, testsJVM % "test-internal -> test")

lazy val bricksJVM = project.in(file(".bricksJVM"))
  .settings(moduleName := "bricks")
  .settings(bricksSettings)
  .settings(commonJvmSettings)
  .aggregate(macrosJVM, platformJVM, scalatestJVM, specs2, testkitJVM, testsJVM, docs)
  .dependsOn(macrosJVM, platformJVM, scalatestJVM, specs2, testkitJVM, testsJVM % "compile;test-internal -> test")

lazy val bricksJS = project.in(file(".bricksJS"))
  .settings(moduleName := "bricks")
  .settings(bricksSettings)
  .settings(commonJsSettings)
  .aggregate(macrosJS, platformJS, scalatestJS, testkitJS, testsJS)
  .dependsOn(macrosJS, platformJS, scalatestJS, testsJS % "test-internal -> test")
  .enablePlugins(ScalaJSPlugin)

/**
 * Macros - cross project that defines macros
 */
lazy val macros = crossProject.crossType(CrossType.Pure)
  .settings(moduleName := "bricks-macros")
  .settings(bricksSettings:_*)
  .settings(libraryDependencies += "org.typelevel" %%% "macro-compat" % macroCompatVersion % "compile")
  .settings(scalaMacroDependencies(paradiseVersion):_*)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val macrosJVM = macros.jvm
lazy val macrosJS = macros.js

/**
 * Platform - cross project that provides cross platform support
 */
lazy val platform = crossProject.crossType(CrossType.Dummy)
  .dependsOn(macros)
  .settings(moduleName := "bricks-platform")
  .settings(bricksSettings:_*)
  .settings(scalaMacroDependencies(paradiseVersion):_*)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val platformJVM = platform.jvm
lazy val platformJS = platform.js

/**
 * Scalatest - cross project that defines test utilities for scalatest
 */
lazy val scalatest = crossProject.crossType(CrossType.Pure)
  .dependsOn(testkit)
  .settings(moduleName := "bricks-scalatest")
  .settings(bricksSettings:_*)
  .settings(disciplineDependencies:_*)
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % scalatestVersion)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val scalatestJVM = scalatest.jvm
lazy val scalatestJS = scalatest.js

/**
 * Specs2 - JVM project that defines test utilities for specs2
 */
lazy val specs2 = project
  .dependsOn(testkitJVM, testsJVM % "test-internal -> compile")
  .settings(moduleName := "bricks-specs2")
  .settings(bricksSettings:_*)
  .settings(disciplineDependencies:_*)
  .settings(libraryDependencies += "org.specs2" %% "specs2-core" % specs2Version)
  .settings(libraryDependencies += "org.specs2" %% "specs2-scalacheck" % specs2Version)
  .settings(commonJvmSettings:_*)

/**
 * Tests - cross project that defines test utilities that can be re-used in other libraries, as well as 
 *         all the tests for this build.
 */
lazy val testkit = crossProject.crossType(CrossType.Pure)
  .dependsOn(macros, platform)
  .settings(moduleName := "bricks-testkit")
  .settings(bricksSettings:_*)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val testkitJVM = testkit.jvm
lazy val testkitJS = testkit.js


/**
 * Tests - cross project that defines test utilities that can be re-used in other libraries, as well as 
 *         all the tests for this build.
 */
lazy val tests = crossProject.crossType(CrossType.Pure)
  .dependsOn(macros, platform, testkit, scalatest % "test-internal -> test")
  .settings(moduleName := "bricks-tests")
  .settings(bricksSettings:_*)
  .settings(disciplineDependencies:_*)
  .settings(noPublishSettings:_*)
  .settings(libraryDependencies += "org.scalatest" %%% "scalatest" % scalatestVersion % "test")
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js

/**
 * Docs - Generates and publishes the scaladoc API documents and the project web site 
 */
lazy val docs = project
  .settings(moduleName := "bricks-docs")
  .settings(bricksSettings)
  .settings(noPublishSettings)
  .settings(unidocSettings)
  .settings(site.settings)
  .settings(ghpages.settings)
  .settings(docSettings)
  .settings(tutSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .settings(doctestSettings)
  .settings(doctestTestFramework := DoctestTestFramework.ScalaTest)
  .settings(doctestWithDependencies := false)
  .settings(commonJvmSettings)
  .dependsOn(platformJVM, macrosJVM, scalatestJVM, specs2, testkitJVM)

lazy val docSettings = Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(platformJVM, macrosJVM, scalatestJVM, specs2, testkitJVM),
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  site.addMappingsToSiteDir(tut, "_tut"),
  ghpagesNoJekyll := false,
  siteMappings += file("CONTRIBUTING.md") -> "contributing.md",
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-diagrams"
  ),
  git.remoteRepo := repo,
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

/**
 * Plugin and other settings
 */
lazy val disciplineDependencies = Seq(
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalacheckVersion,
  libraryDependencies += "org.typelevel" %%% "discipline" % disciplineVersion
)

lazy val publishSettings = sharedPublishSettings(home, repo, api, license) ++ Seq(
  autoAPIMappings := true,
  pomExtra := (
    <developers>
      <developer>
        <id>inthenow</id>
        <name>Alistair Johnson</name>
        <url>http://github.com/InTheNow/</url>
      </developer>
    </developers>
  )
) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageMinimum := 60,
  ScoverageKeys.coverageFailOnMinimum := false,
  ScoverageKeys.coverageHighlighting := scalaBinaryVersion.value != "2.10"
 // ScoverageKeys.coverageExcludedPackages := "bricks\\.bench\\..*"
)
