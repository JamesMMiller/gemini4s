val scala3Version = "3.3.1"
val zioVersion = "2.0.19"
val zioHttpVersion = "3.0.0-RC3"
val zioJsonVersion = "0.6.2"

inThisBuild(
  List(
    scalaVersion := scala3Version,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "gemini4s",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test cross CrossVersion.for3Use2_13
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    
    // Scoverage settings
    coverageEnabled := true,
    coverageMinimumBranchTotal := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*",

    addCommandAlias("lint", ";scalafixAll --check"),
    addCommandAlias("lintFix", ";scalafixAll")
  ) 