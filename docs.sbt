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
          navLinks = Seq(
            IconLink.internal(Root / "roadmap.md", HeliumIcon.demo),
            IconLink.internal(Root / "contributing.md", HeliumIcon.info)
          )
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
              TextLink.internal(Root / "files.md", "File API"),
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
            ),
            ThemeNavigationSection(
              "Project",
              TextLink.internal(Root / "roadmap.md", "Roadmap"),
              TextLink.internal(Root / "contributing.md", "Contributing"),
              TextLink.internal(Root / "api-compliance.md", "API Compliance")
            )
          )
        )
    },

    // Link to API documentation
    tlSiteApiUrl := Some(url("https://javadoc.io/doc/io.github.jamesmmiller/gemini4s_3")),

    // GitHub Pages settings
    tlSitePublishBranch := Some("gh-pages")
  )
  .dependsOn(LocalProject("root"))
