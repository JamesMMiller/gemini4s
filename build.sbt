val scala3Version = "3.6.2"
val zioCliVersion = "0.5.0"

inThisBuild(
  List(
    scalaVersion      := scala3Version,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    coverageEnabled   := true
  )
)

lazy val commonSettings = Seq(
  scalaVersion := scala3Version,
  libraryDependencies ++= Seq(
    "org.typelevel"                 %% "cats-effect"         % "3.5.2",
    "co.fs2"                        %% "fs2-core"            % "3.9.3",
    "co.fs2"                        %% "fs2-io"              % "3.9.3",
    "com.softwaremill.sttp.client3" %% "core"                % "3.9.1",
    "com.softwaremill.sttp.client3" %% "fs2"                 % "3.9.1",
    "com.softwaremill.sttp.client3" %% "circe"               % "3.9.1",
    "io.circe"                      %% "circe-core"          % "0.14.6",
    "io.circe"                      %% "circe-generic"       % "0.14.6",
    "io.circe"                      %% "circe-parser"        % "0.14.6",
    "org.scalameta"                 %% "munit"               % "0.7.29" % Test,
    "org.typelevel"                 %% "munit-cats-effect-3" % "1.0.7"  % Test
  ),
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name    := "gemini4s",
    version := "0.1.0-SNAPSHOT",

    // Publishing settings
    organization := "com.github.jamesmiller",
    homepage     := Some(url("https://github.com/JamesMMiller/gemini4s")),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers   := List(
      Developer(
        "jamesmiller",
        "James Miller",
        "james@example.com",
        url("https://github.com/JamesMMiller")
      )
    ),
    scmInfo      := Some(
      ScmInfo(
        url("https://github.com/JamesMMiller/gemini4s"),
        "scm:git:git@github.com:JamesMMiller/gemini4s.git"
      )
    ),

    // Scoverage settings
    coverageMinimumStmtTotal := 90,
    coverageMinimumBranchTotal := 90,
    coverageFailOnMinimum    := true,
    coverageHighlighting     := true,
    coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*",
    addCommandAlias("lint", ";scalafixAll --check; testCoverage"),
    addCommandAlias("lintFix", ";scalafixAll"),
    addCommandAlias("testCoverage", ";clean;coverage;test;coverageReport"),

    // Load .env file for tests
    Test / fork    := true,
    Test / envVars := {
      val envFile = file(".env")
      if (envFile.exists()) {
        val props = new java.util.Properties()
        val is    = new java.io.FileInputStream(envFile)
        try props.load(is)
        finally is.close()
        import scala.collection.JavaConverters._
        props.asScala.toMap.map { case (k, v) => k -> v.trim }
      } else {
        Map.empty
      }
    }
  )

// Examples project removed as it was ZIO-based
