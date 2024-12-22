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

lazy val commonSettings = Seq(
  scalaVersion := scala3Version,
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
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "gemini4s",
    version := "0.1.0-SNAPSHOT",
    
    // Scoverage settings
    coverageEnabled := true,
    coverageMinimumBranchTotal := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*",

    addCommandAlias("lint", ";scalafixAll --check"),
    addCommandAlias("lintFix", ";scalafixAll")
  )

lazy val examples = project
  .in(file("examples"))
  .settings(
    commonSettings,
    name := "gemini4s-examples",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-cli" % "0.5.0"  // For CLI application
    ),
    
    // Assembly settings
    assembly / mainClass := Some("gemini4s.examples.GeminiCli"),
    assembly / assemblyJarName := "gemini4s-cli.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        (xs map {_.toLowerCase}) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
            MergeStrategy.discard
          case _ => MergeStrategy.first
        }
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    }
  )
  .dependsOn(root)
  