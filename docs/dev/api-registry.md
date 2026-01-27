# API Cumulative Registry

> Auto-generated on first story creation
> Updated by Dev Agent after each story completion

## Registry Metadata

**Last Updated**: 2026-01-28
**Total Stories Tracked**: 2
**Total Endpoints**: 11
**Repository**: shop-video-scout
**Mode**: monolith

## API Endpoints Registry

### task-service (port 8082)

| Method | Path | Controller | Auth | Request | Response | Story |
|--------|------|------------|------|---------|----------|-------|
| POST | `/api/v1/tasks/{id}/compose` | ComposeController | X-User-Id | — | `R<ComposeResponse>` | 4.1 |
| GET | `/api/v1/tasks/{id}/compose-progress` | ComposeController | X-User-Id | — | `R<ComposeProgressResponse>` | 4.1 |
| PUT | `/api/v1/tasks/{id}/voice-type` | VoiceTypeController | X-User-Id | `VoiceTypeRequest` | `R<Void>` | 4.1 |
| POST | `/internal/tasks/{taskId}/compose-complete` | ComposeCallbackController | internal | `Map` | `R<Void>` | 4.1 |

### user-service (port 8081)

| Method | Path | Controller | Auth | Request | Response | Story |
|--------|------|------------|------|---------|----------|-------|
| POST | `/api/v1/voice/upload-url` | VoiceSampleController | X-User-Id | `VoiceUploadUrlRequest` | `R<VoiceUploadUrlResponse>` | 4.2 |
| POST | `/api/v1/voice/samples` | VoiceSampleController | X-User-Id | `CreateVoiceSampleRequest` | `R<VoiceSampleResponse>` (201) | 4.2 |
| GET | `/api/v1/voice/samples` | VoiceSampleController | X-User-Id | — | `R<List<VoiceSampleResponse>>` | 4.2 |
| GET | `/api/v1/voice/samples/{id}` | VoiceSampleController | X-User-Id | — | `R<VoiceSampleResponse>` | 4.2 |
| DELETE | `/api/v1/voice/samples/{id}` | VoiceSampleController | X-User-Id | — | `R<Void>` | 4.2 |
| GET | `/api/v1/voice/samples/{id}/preview` | VoiceSampleController | X-User-Id | — | `R<VoicePreviewResponse>` | 4.2 |
| POST | `/api/v1/voice/samples/{id}/clone-result` | VoiceSampleController | internal | `CloneResultRequest` | `R<Void>` | 4.2 |

## Endpoints by Story

| Story | Endpoints Added | Service |
|-------|-----------------|---------|
| 4.1 | POST /compose, GET /compose-progress, PUT /voice-type, POST /compose-complete (internal) | task-service |
| 4.2 | POST /upload-url, POST /samples, GET /samples, GET /samples/{id}, DELETE /samples/{id}, GET /samples/{id}/preview, POST /samples/{id}/clone-result (internal) | user-service |
