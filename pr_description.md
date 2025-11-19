# Comprehensive Documentation for gemini4s

## Summary

This PR adds comprehensive, best-of-class documentation for gemini4s following Typelevel and Circe documentation patterns. The documentation includes a fully functional microsite with type-checked examples using mdoc and Laika.

## Changes

### Documentation Microsite

- **Added sbt-typelevel-site plugin** for documentation generation
- **Configured Laika** with Helium theme for static site generation
- **Set up mdoc** for type-checked Scala code examples in documentation
- **Created docs subproject** in build.sbt for site generation

### Documentation Pages (14 total)

1. **index.md** - Main entry point with motivation, features, and navigation
2. **quickstart.md** - Getting started guide with installation and first API call
3. **core-concepts.md** - Tagless Final, effect types, error hierarchy, configuration
4. **content-generation.md** - Generation parameters, JSON mode, conversations
5. **streaming.md** - FS2 streaming patterns, chatbot example, backpressure
6. **function-calling.md** - Tool use, function declarations, complete flow
7. **safety.md** - Content filtering, harm categories, safety settings
8. **error-handling.md** - Error hierarchy, retry strategies, circuit breakers
9. **embeddings.md** - Semantic search, clustering, batch processing
10. **models.md** - Model comparison and selection guide
11. **best-practices.md** - Production patterns, resource management, security
12. **faq.md** - Common questions and troubleshooting
13. **examples.md** - Complete working examples (chatbot, streaming)
14. **caching.md** - Context caching for cost optimization

### Enhanced README

- Added badges (Maven Central, Javadocs)
- Improved structure with "Why gemini4s?" section
- Better feature showcase with code examples
- Links to comprehensive documentation site
- Acknowledgments section for Typelevel ecosystem

### GitHub Actions Workflow

- **docs.yml** - Automated documentation deployment to GitHub Pages
- Builds on every push to main and PRs
- Deploys to GitHub Pages on main branch merges

## Documentation Features

- ✅ **Type-checked examples** - All code examples verified by mdoc at compile time
- ✅ **Comprehensive coverage** - All library features documented
- ✅ **Practical examples** - Real-world use cases and patterns
- ✅ **Best practices** - Production-ready patterns and recommendations
- ✅ **Error handling** - Retry strategies, circuit breakers, graceful degradation
- ✅ **Typelevel style** - Follows Circe/Cats/FS2 documentation patterns

## Testing

The microsite can be previewed locally with:
```bash
sbt docs/tlSitePreview
```

Then visit http://localhost:4242

## Deployment

Once merged to main, the documentation will be automatically deployed to:
https://jamesmmiller.github.io/gemini4s/

## Checklist

- [x] Created comprehensive documentation pages
- [x] Set up microsite with sbt-typelevel-site
- [x] Configured mdoc for type-checked examples
- [x] Added GitHub Actions workflow for deployment
- [x] Enhanced README with better structure
- [x] All documentation follows Typelevel patterns
- [x] Code examples are complete and working
- [x] Links between pages are correct
- [x] Committed on feature branch
- [x] Ready for review

## Notes

The documentation is modular and can be easily extended with additional pages. All code examples use mdoc's `compile-only` mode to ensure they type-check without requiring runtime execution or API keys.

The microsite uses Laika's Helium theme which provides a clean, professional look consistent with other Typelevel projects.