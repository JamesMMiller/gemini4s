# Roadmap

This document outlines the planned features and development phases for `gemini4s`.

## Phase 1: Multimodal Support (Current)
We are currently working on enabling multimodal inputs for the Gemini API.

- [ ] **Refactor `ContentPart`**: Support `Text`, `InlineData` (Base64 images), and `FileData` (URI references).
- [ ] **Image Support**: Allow sending images to Gemini 1.5 Flash/Pro models.
- [ ] **Helper Methods**: Simplify creating multimodal requests.

## Phase 2: Image Generation (Imagen)
Support for Google's Imagen models to generate images from text.

- [ ] **Imagen 3 Support**: Implement `v1beta/models/imagen-3.0-generate-001:predict`.
- [ ] **Image Editing**: Support text-and-image-to-image editing.

## Phase 3: Files API
Support for the Gemini Files API to handle large media files.

- [ ] **Upload Files**: `POST /upload/v1beta/files`.
- [ ] **List/Get/Delete Files**: Manage uploaded resources.
- [ ] **Integration**: Seamlessly use uploaded file URIs in content generation.

## Phase 4: Audio & Video
Enhanced support for audio and video modalities.

- [ ] **Audio Understanding**: Transcribe and analyze audio files.
- [ ] **Video Understanding**: Process video content with Gemini 1.5 Pro.
- [ ] **Real-time Streaming**: Support for multimodal real-time API (if applicable via REST/WebSocket).

## Future Ideas
- **Grounding**: Integration with Google Search for grounded responses.
- **Semantic Retrieval**: Advanced RAG capabilities.
- **Tuning**: Support for model tuning API.
