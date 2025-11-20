val baseVersion   = "0.0"
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
    "com.softwaremill.sttp.client3" %% "core"                % "3.9.8",
    "com.softwaremill.sttp.client3" %% "fs2"                 % "3.9.8",
    "com.softwaremill.sttp.client3" %% "circe"               % "3.9.8",
    "io.circe"                      %% "circe-core"          % "0.14.6",
    "io.circe"                      %% "circe-generic"       % "0.14.6",
    "io.circe"                      %% "circe-parser"        % "0.14.6",
    "io.circe"                      %% "circe-fs2"           % "0.14.1",
    "org.scalameta"                 %% "munit"               % "0.7.29" % Test,
    "org.typelevel"                 %% "munit-cats-effect-3" % "1.0.7"  % Test
  ),
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val root = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "gemini4s",

    // Publishing settings
    organization           := "io.github.jamesmmiller",
    homepage               := Some(url("https://github.com/JamesMMiller/gemini4s")),
    licenses               := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    sonatypeCredentialHost := "central.sonatype.com",
    publishTo              := {
      if (isSnapshot.value) {
        Some("snapshots" at "https://central.sonatype.com/repository/maven-snapshots/")
      } else {
        sonatypePublishToBundle.value
      }
    },
    developers             := List(
      Developer(
        "jamesmiller",
        "James Miller",
        "james@example.com",
        url("https://github.com/JamesMMiller")
      )
    ),
    scmInfo                := Some(
      ScmInfo(
        url("https://github.com/JamesMMiller/gemini4s"),
        "scm:git:git@github.com:JamesMMiller/gemini4s.git"
      )
    ),

    // Scoverage settings
    coverageMinimumStmtTotal   := 80,
    coverageMinimumBranchTotal := 90,
    coverageFailOnMinimum      := true,
    coverageHighlighting       := true,
    coverageExcludedPackages   := "<empty>;Reverse.*;.*AuthService.*;models\\.data\\..*",
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

// Documentation subproject
lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    name := "gemini4s-docs",

    // mdoc settings for type-checked examples
    mdocIn        := file("docs"),
    mdocVariables := Map(
      "VERSION" -> {
        if (isSnapshot.value) {
          // Try to get the last stable version, falling back to git describe, then current version
          previousStableVersion.value
            .orElse(scala.util.Try(scala.sys.process.Process("git describe --tags --abbrev=0").!!.trim).toOption)
            .getOrElse(version.value)
        } else {
          version.value
        }
      }
    ),

    // Site configuration
    tlSiteHelium := {
      import laika.helium.config._
      import laika.ast.Path.Root

      tlSiteHelium.value.site
        .metadata(
          title = Some("gemini4s"),
          description = Some("A Tagless Final Scala library for the Google Gemini API"),
          language = Some("en")
        )
        .site
        .topNavigationBar(
          homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home),
          navLinks = Seq()
        )
        .site
        .mainNavigation(
          0,
          false,
          Seq(
            ThemeNavigationSection(
              "Getting Started",
              TextLink.internal(Root / "quickstart.md", "Quick Start")
            ),
            ThemeNavigationSection(
              "Core Concepts",
              TextLink.internal(Root / "core-concepts.md", "Fundamentals"),
              TextLink.internal(Root / "models.md", "Models"),
              TextLink.internal(Root / "error-handling.md", "Error Handling")
            ),
            ThemeNavigationSection(
              "Features",
              TextLink.internal(Root / "content-generation.md", "Content Generation"),
              TextLink.internal(Root / "streaming.md", "Streaming"),
              TextLink.internal(Root / "function-calling.md", "Function Calling"),
              TextLink.internal(Root / "embeddings.md", "Embeddings"),
              TextLink.internal(Root / "safety.md", "Safety Settings"),
              TextLink.internal(Root / "caching.md", "Context Caching")
            ),
            ThemeNavigationSection(
              "Reference",
              TextLink.internal(Root / "examples.md", "Examples"),
              TextLink.internal(Root / "best-practices.md", "Best Practices"),
              TextLink.internal(Root / "faq.md", "FAQ")
            )
          )
        )
    },

    // Link to API documentation
    tlSiteApiUrl := Some(url("https://javadoc.io/doc/io.github.jamesmmiller/gemini4s_3")),

    // GitHub Pages settings
    tlSitePublishBranch := Some("gh-pages")
  )
  .dependsOn(root)

// Examples project removed as it was ZIO-based
