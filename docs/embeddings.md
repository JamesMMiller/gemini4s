# Embeddings

Generate embeddings for semantic search, clustering, and similarity tasks.

## What are Embeddings?

Embeddings are vector representations of text that capture semantic meaning. Use them for:
- Semantic search
- Document clustering
- Recommendation systems
- Similarity detection
- Classification

## Basic Embedding

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.config.GeminiConfig

def basicEmbedding(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  service.embedContent(
    content = GeminiService.text("Scala is a programming language")
  ).flatMap {
    case Right(embedding) =>
      IO.println(s"Embedding dimension: ${embedding.values.length}") *>
      IO.println(s"First few values: ${embedding.values.take(5)}")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Task Types

Specify the task type for optimized embeddings:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.TaskType
import gemini4s.config.GeminiConfig

def withTaskType(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  // For search queries
  service.embedContent(
    content = GeminiService.text("best scala libraries"),
    taskType = Some(TaskType.RETRIEVAL_QUERY)
  ).void
  
  // For documents to be searched
  service.embedContent(
    content = GeminiService.text("Cats Effect is a library for..."),
    taskType = Some(TaskType.RETRIEVAL_DOCUMENT),
    title = Some("Cats Effect Documentation")
  ).void
  
  // For similarity comparison
  service.embedContent(
    content = GeminiService.text("functional programming"),
    taskType = Some(TaskType.SEMANTIC_SIMILARITY)
  ).void
}
```

## Batch Embeddings

Embed multiple texts efficiently:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.{EmbedContentRequest, TaskType}
import gemini4s.config.GeminiConfig

def batchEmbeddings(service: GeminiService[IO])(using GeminiConfig): IO[Unit] = {
  val documents = List(
    "Scala is a functional programming language",
    "Cats Effect provides IO monad",
    "FS2 is a streaming library"
  )
  
  val requests = documents.map { doc =>
    EmbedContentRequest(
      content = GeminiService.text(doc),
      model = s"models/${GeminiService.EmbeddingText004}",
      taskType = Some(TaskType.RETRIEVAL_DOCUMENT)
    )
  }
  
  service.batchEmbedContents(requests).flatMap {
    case Right(embeddings) =>
      IO.println(s"Generated ${embeddings.length} embeddings")
    case Left(error) =>
      IO.println(s"Error: ${error.message}")
  }
}
```

## Cosine Similarity

Calculate similarity between embeddings:

```scala mdoc:compile-only
import gemini4s.model.GeminiResponse.ContentEmbedding

def cosineSimilarity(a: ContentEmbedding, b: ContentEmbedding): Double = {
  require(a.values.length == b.values.length, "Embeddings must have same dimension")
  
  val dotProduct = a.values.zip(b.values).map { case (x, y) => x * y }.sum
  val magnitudeA = math.sqrt(a.values.map(x => x * x).sum)
  val magnitudeB = math.sqrt(b.values.map(x => x * x).sum)
  
  dotProduct / (magnitudeA * magnitudeB)
}
```

## Semantic Search Example

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.{EmbedContentRequest, TaskType}
import gemini4s.model.GeminiResponse.ContentEmbedding
import gemini4s.config.GeminiConfig

case class Document(id: String, text: String, embedding: ContentEmbedding)

def cosineSimilarity(a: ContentEmbedding, b: ContentEmbedding): Double = {
  require(a.values.length == b.values.length, "Embeddings must have same dimension")
  val dotProduct = a.values.zip(b.values).map { case (x, y) => x * y }.sum
  val magnitudeA = math.sqrt(a.values.map(x => x * x).sum)
  val magnitudeB = math.sqrt(b.values.map(x => x * x).sum)
  dotProduct / (magnitudeA * magnitudeB)
}

def semanticSearch(
  service: GeminiService[IO],
  documents: List[Document],
  query: String
)(using GeminiConfig): IO[List[(Document, Double)]] = {
  service.embedContent(
    content = GeminiService.text(query),
    taskType = Some(TaskType.RETRIEVAL_QUERY)
  ).flatMap {
    case Right(queryEmbedding) =>
      // Calculate similarities
      val results: List[(Document, Double)] = documents.map { doc =>
        val similarity: Double = cosineSimilarity(queryEmbedding, doc.embedding)
        (doc, similarity)
      }.sortBy((pair: (Document, Double)) => -pair._2)  // Sort by similarity descending
      IO.pure(results)
    case Left(error) =>
      IO.raiseError(new RuntimeException(error.message))
  }
}
```

## Document Clustering

Simplified example (use a proper ML library in production):

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.{EmbedContentRequest, TaskType}
import gemini4s.model.GeminiResponse.ContentEmbedding
import gemini4s.config.GeminiConfig

def clusterDocuments(
  service: GeminiService[IO],
  documents: List[String],
  k: Int  // number of clusters
)(using GeminiConfig): IO[Map[Int, List[String]]] = {
  val requests: List[EmbedContentRequest] = documents.map { doc =>
    EmbedContentRequest(
      content = GeminiService.text(doc),
      model = s"models/${GeminiService.EmbeddingText004}",
      taskType = Some(TaskType.CLUSTERING)
    )
  }
  
  service.batchEmbedContents(requests).flatMap {
    case Right(embeddings) =>
      // Simple clustering: group by index modulo k
      val clusters: Map[Int, List[String]] = documents.zipWithIndex
        .groupBy { case (_, idx) => idx % k }
        .map { case (cluster, items) => cluster -> items.map(_._1) }
      IO.pure(clusters)
    case Left(error) =>
      IO.raiseError(new RuntimeException(error.message))
  }
}
```

## Best Practices

### 1. Use Appropriate Task Types

```scala mdoc:compile-only
import gemini4s.model.GeminiRequest.TaskType

// For search queries
val queryTask = TaskType.RETRIEVAL_QUERY

// For documents in a search corpus
val documentTask = TaskType.RETRIEVAL_DOCUMENT

// For comparing similarity
val similarityTask = TaskType.SEMANTIC_SIMILARITY

// For clustering
val clusteringTask = TaskType.CLUSTERING

// For classification
val classificationTask = TaskType.CLASSIFICATION
```

### 2. Cache Embeddings

Embeddings are expensive - cache them:

```scala mdoc:compile-only
import cats.effect.{IO, Ref}
import gemini4s.GeminiService
import gemini4s.model.GeminiResponse.ContentEmbedding
import gemini4s.config.GeminiConfig

case class EmbeddingCache(
  cache: Ref[IO, Map[String, ContentEmbedding]]
) {
  def getOrCompute(
    service: GeminiService[IO],
    text: String
  )(using GeminiConfig): IO[ContentEmbedding] = {
    cache.get.flatMap { cached =>
      cached.get(text) match {
        case Some(embedding) => IO.pure(embedding)
        case None =>
          service.embedContent(
            content = GeminiService.text(text)
          ).flatMap {
            case Right(embedding) =>
              cache.update(_ + (text -> embedding)) *>
              IO.pure(embedding)
            case Left(error) =>
              IO.raiseError(new RuntimeException(error.message))
          }
      }
    }
  }
}
```

### 3. Normalize Vectors

For better similarity comparison:

```scala mdoc:compile-only
import gemini4s.model.GeminiResponse.ContentEmbedding

def normalize(embedding: ContentEmbedding): ContentEmbedding = {
  val magnitude = math.sqrt(embedding.values.map(x => x * x).sum)
  ContentEmbedding(embedding.values.map(x => (x / magnitude).toFloat))
}
```

### 4. Batch When Possible

Use batch embeddings for efficiency:

```scala mdoc:compile-only
import cats.effect.IO
import gemini4s.GeminiService
import gemini4s.model.GeminiRequest.EmbedContentRequest
import gemini4s.config.GeminiConfig

def efficientEmbedding(
  service: GeminiService[IO],
  texts: List[String]
)(using GeminiConfig): IO[Unit] = {
  // Good - batch request
  val requests = texts.map { text =>
    EmbedContentRequest(
      content = GeminiService.text(text),
      model = s"models/${GeminiService.EmbeddingText004}"
    )
  }
  service.batchEmbedContents(requests).void
  
  // Avoid - individual requests
  // texts.traverse(text => service.embedContent(GeminiService.text(text)))
}
```

## Next Steps

- **[Examples](examples.md)** - Complete embedding examples
- **[Best Practices](best-practices.md)** - Production patterns
