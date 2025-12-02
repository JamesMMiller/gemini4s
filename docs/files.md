# File API

The File API allows you to upload files to Google's servers and use them in your prompts. This is useful for multimodal generation where you want to include images, PDFs, or other file types that are too large to be included directly in the request or that you want to reuse across multiple requests.

## Uploading Files

To upload a file, use the `uploadFile` method. This method supports resumable uploads, making it suitable for large files.

```scala
import gemini4s.GeminiService
import java.nio.file.Paths

def uploadExample(service: GeminiService[IO]): IO[Unit] = {
  val path = Paths.get("path/to/image.png")
  service.uploadFile(path, "image/png", Some("My Image")).flatMap {
    case Right(file) => IO.println(s"Uploaded file: ${file.uri}")
    case Left(error) => IO.println(s"Upload failed: $error")
  }
}
```

## Listing Files

You can list uploaded files using the `listFiles` method.

```scala
def listFilesExample(service: GeminiService[IO]): IO[Unit] = {
  service.listFiles().flatMap {
    case Right(response) => 
      response.files.foreach(file => println(s"${file.displayName}: ${file.uri}"))
    case Left(error) => IO.println(s"List failed: $error")
  }
}
```

## Getting File Details

To get details about a specific file, use the `getFile` method with the file's resource name (e.g., `files/12345`).

```scala
def getFileExample(service: GeminiService[IO], fileName: String): IO[Unit] = {
  service.getFile(fileName).flatMap {
    case Right(file) => IO.println(s"File details: $file")
    case Left(error) => IO.println(s"Get failed: $error")
  }
}
```

## Deleting Files

To delete a file, use the `deleteFile` method with the file's resource name.

```scala
def deleteFileExample(service: GeminiService[IO], fileName: String): IO[Unit] = {
  service.deleteFile(fileName).flatMap {
    case Right(_) => IO.println("File deleted successfully")
    case Left(error) => IO.println(s"Delete failed: $error")
  }
}
```

## Using Files in Prompts

Once a file is uploaded, you can use its URI in your prompts using `GeminiService.file`.

```scala
def generateWithFile(service: GeminiService[IO], fileUri: String): IO[Unit] = {
  val content = GeminiService.file(fileUri, "image/png")
  val request = GenerateContentRequest(
    model = ModelName.Standard("gemini-1.5-flash"),
    contents = List(content)
  )
  
  service.generateContent(request).flatMap {
    case Right(response) => IO.println(s"Generated: ${response.candidates.head.content}")
    case Left(error) => IO.println(s"Generation failed: $error")
  }
}
```
