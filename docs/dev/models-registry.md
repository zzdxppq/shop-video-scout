# Models & Types Cumulative Registry

> Auto-generated on first story creation
> Updated by Dev Agent after each story completion

## Registry Metadata

**Last Updated**: 2026-01-28
**Total Stories Tracked**: 3
**Total Models**: 4
**Repository**: shop-video-scout
**Mode**: monolith

## Java Classes

### Response Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| R<T> | Response Wrapper | common-core/.../result/R.java | 1.1 |
| ResultCode | Enum | common-core/.../result/ResultCode.java | 1.1 |

### MQ Message Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| ComposeMessage | MQ DTO | common-core/.../mq/ComposeMessage.java | 4.1 |
| ComposeMessage.Paragraph | Inner DTO | common-core/.../mq/ComposeMessage.java | 4.1 |
| ComposeMessage.VoiceConfig | Inner DTO | common-core/.../mq/ComposeMessage.java | 4.1 (ext 4.2: +voiceSampleId) |
| MqConstants | Constants | common-core/.../mq/MqConstants.java | 4.1 (ext 4.2: +voice clone constants) |
| VoiceCloneMessage | MQ DTO | common-core/.../mq/VoiceCloneMessage.java | 4.2 |

### Exception Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| BusinessException | Exception | common-core/.../exception/BusinessException.java | 1.1 |
| GlobalExceptionHandler | Handler | common-core/.../exception/GlobalExceptionHandler.java | 1.1 |

### Entity Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| BaseEntity | Abstract Entity | common-mybatis/.../entity/BaseEntity.java | 1.1 |
| VoiceSample | Entity | user-service/.../entity/VoiceSample.java | 4.2 |

### DTO Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| ComposeResponse | Response DTO | task-service/.../dto/ComposeResponse.java | 4.1 |
| ComposeProgressResponse | Response DTO | task-service/.../dto/ComposeProgressResponse.java | 4.1 |
| VoiceTypeRequest | Request DTO | task-service/.../dto/VoiceTypeRequest.java | 4.1 |
| ComposeRequest | Request DTO | task-service/.../dto/ComposeRequest.java | 4.1 |
| VoiceUploadUrlRequest | Request DTO | user-service/.../dto/VoiceUploadUrlRequest.java | 4.2 |
| VoiceUploadUrlResponse | Response DTO | user-service/.../dto/VoiceUploadUrlResponse.java | 4.2 |
| CreateVoiceSampleRequest | Request DTO | user-service/.../dto/CreateVoiceSampleRequest.java | 4.2 |
| VoiceSampleResponse | Response DTO | user-service/.../dto/VoiceSampleResponse.java | 4.2 |
| CloneResultRequest | Request DTO | user-service/.../dto/CloneResultRequest.java | 4.2 |
| VoicePreviewResponse | Response DTO | user-service/.../dto/VoicePreviewResponse.java | 4.2 |

### Configuration Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| RedisConfig | Config | common-core/.../config/RedisConfig.java | 1.1 |
| MybatisPlusConfig | Config | common-mybatis/.../config/MybatisPlusConfig.java | 1.1 |
| RateLimitConfig | Config | gateway-service/.../config/RateLimitConfig.java | 1.1 |
| RabbitMqConfig | Config | task-service/.../config/RabbitMqConfig.java | 4.1 |
| RabbitMqConfig | Config | media-service/.../config/RabbitMqConfig.java | 4.1 (ext 4.2: +voice clone queues) |
| RabbitMqConfig | Config | user-service/.../config/RabbitMqConfig.java | 4.2 |
| OssConfig | Config | user-service/.../config/OssConfig.java | 4.2 |
| VolcanoTtsProperties | Properties | media-service/.../config/VolcanoTtsProperties.java | 4.1 |
| OssConfig | Config | media-service/.../config/OssConfig.java | 4.1 |
| ComposeProperties | Properties | media-service/.../config/ComposeProperties.java | 4.1 |
| MediaServiceConfig | Config | media-service/.../config/MediaServiceConfig.java | 4.1 |

### Utility Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| RedisUtils | Utility | common-core/.../util/RedisUtils.java | 1.1 |
| JwtUtils | Utility | common-security/.../jwt/JwtUtils.java | 1.1 |
| JwtProperties | Properties | common-security/.../jwt/JwtProperties.java | 1.1 |

## Enums & Constants

### ResultCode Values
| Value | Code | Message | Story |
|-------|------|---------|-------|
| SUCCESS | 200 | Success | 1.1 |
| BAD_REQUEST | 400 | Bad request | 1.1 |
| UNAUTHORIZED | 401 | Unauthorized | 1.1 |
| FORBIDDEN | 403 | Forbidden | 1.1 |
| NOT_FOUND | 404 | Resource not found | 1.1 |
| VALIDATION_ERROR | 422 | Validation error | 1.1 |
| INTERNAL_ERROR | 500 | Internal server error | 1.1 |
| SERVICE_UNAVAILABLE | 503 | Service unavailable | 1.1 |
| GATEWAY_TIMEOUT | 504 | Gateway timeout | 1.1 |
| USER_NOT_FOUND | 1001 | User not found | 1.1 |
| TASK_NOT_FOUND | 1002 | Task not found | 1.1 |
| VIDEO_NOT_FOUND | 1003 | Video not found | 1.1 |
| INVALID_FILE_FORMAT | 1004 | Invalid file format | 1.1 |
| FILE_SIZE_EXCEEDED | 1005 | File size exceeded | 1.1 |
| VIDEO_DURATION_EXCEEDED | 1006 | Video duration exceeded | 1.1 |
| VIDEO_COUNT_EXCEEDED | 1007 | Video count exceeded | 1.1 |
| AI_SERVICE_ERROR | 1008 | AI service error | 1.1 |
| AI_SERVICE_TIMEOUT | 1009 | AI service timeout | 1.1 |
| TTS_SERVICE_ERROR | 1010 | TTS service error | 4.1 |
| TTS_SERVICE_TIMEOUT | 1011 | TTS service timeout | 4.1 |
| INVALID_VOICE_TYPE | 1012 | Invalid voice type | 4.1 |
| COMPOSE_TEXT_TOO_LONG | 1013 | Compose text too long | 4.1 |
| TASK_STATUS_INVALID | 1014 | Task status invalid | 4.1 |
| TASK_ALREADY_COMPOSING | 1015 | Task already composing | 4.1 |
| SCRIPT_NOT_FOUND | 1016 | Script not found | 4.1 |
| VOICE_SAMPLE_NOT_FOUND | 1017 | 声音样本未找到 | 4.2 |
| VOICE_SAMPLE_LIMIT_EXCEEDED | 1018 | 声音样本数量已达上限（最多3个） | 4.2 |
| VOICE_CLONE_FAILED | 1019 | 声音克隆失败，请确保样本清晰无杂音 | 4.2 |
| VOICE_CLONE_IN_PROGRESS | 1020 | 您的声音样本正在处理中，请稍后 | 4.2 |
| INVALID_AUDIO_FORMAT | 1021 | 不支持的音频格式，请上传MP3、WAV或M4A文件 | 4.2 |
| AUDIO_DURATION_INVALID | 1022 | 声音样本时长需要在5秒到2分钟之间 | 4.2 |

## Naming Conventions & Patterns

| Pattern | Convention |
|---------|------------|
| Class naming | PascalCase |
| Package naming | lowercase |
| Constant naming | UPPER_SNAKE_CASE |

### Service Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| ComposeService | Service | task-service/.../service/ComposeService.java | 4.1 |
| ComposeProgressService | Service | task-service/.../service/ComposeProgressService.java | 4.1 |
| TtsSynthesisService | Service | media-service/.../service/TtsSynthesisService.java | 4.1 |
| ComposeProgressTracker | Service | media-service/.../service/ComposeProgressTracker.java | 4.1 |
| TaskCallbackClient | Client | media-service/.../service/TaskCallbackClient.java | 4.1 |
| VolcanoTtsClient | Client | media-service/.../client/VolcanoTtsClient.java | 4.1 |
| ComposeMessagePublisher | MQ Publisher | task-service/.../mq/ComposeMessagePublisher.java | 4.1 |
| ComposeMessageConsumer | MQ Consumer | media-service/.../mq/ComposeMessageConsumer.java | 4.1 |
| VoiceSampleService | Service Interface | user-service/.../service/VoiceSampleService.java | 4.2 |
| VoiceSampleServiceImpl | Service | user-service/.../service/impl/VoiceSampleServiceImpl.java | 4.2 |
| VoiceSampleController | Controller | user-service/.../controller/VoiceSampleController.java | 4.2 |
| VoiceSampleMapper | Mapper | user-service/.../mapper/VoiceSampleMapper.java | 4.2 |
| VoiceCloneMessagePublisher | MQ Publisher | user-service/.../mq/VoiceCloneMessagePublisher.java | 4.2 |
| VoiceCloneService | Service | media-service/.../service/VoiceCloneService.java | 4.2 |
| VoiceCloneCallbackClient | Client | media-service/.../service/VoiceCloneCallbackClient.java | 4.2 |
| VoiceCloneMessageConsumer | MQ Consumer | media-service/.../mq/VoiceCloneMessageConsumer.java | 4.2 |
| VoiceSampleReadMapper | Mapper | media-service/.../mapper/VoiceSampleReadMapper.java | 4.2 |

### Constants
| Class | Type | File | Story |
|-------|------|------|-------|
| VoiceConstants | Constants | task-service/.../constant/VoiceConstants.java | 4.1 |

## Models by Story

| Story | Models Added |
|-------|--------------|
| 1.1 | R, ResultCode, BusinessException, GlobalExceptionHandler, BaseEntity, RedisConfig, MybatisPlusConfig, RedisUtils, JwtUtils, JwtProperties, RateLimitConfig |
| 4.1 | ComposeMessage, MqConstants, ComposeResponse, ComposeProgressResponse, VoiceTypeRequest, ComposeRequest, RabbitMqConfig (×2), VolcanoTtsProperties, OssConfig, ComposeProperties, MediaServiceConfig, ComposeService, ComposeProgressService, TtsSynthesisService, ComposeProgressTracker, TaskCallbackClient, VolcanoTtsClient, ComposeMessagePublisher, ComposeMessageConsumer, VoiceConstants, ScriptMapper, ComposeController, VoiceTypeController, ComposeCallbackController |
| 4.2 | VoiceCloneMessage, VoiceSample, VoiceSampleMapper, VoiceSampleService, VoiceSampleServiceImpl, VoiceSampleController, VoiceUploadUrlRequest, VoiceUploadUrlResponse, CreateVoiceSampleRequest, VoiceSampleResponse, CloneResultRequest, VoicePreviewResponse, VoiceCloneMessagePublisher, RabbitMqConfig (user), OssConfig (user), VoiceCloneService, VoiceCloneCallbackClient, VoiceCloneMessageConsumer, VoiceSampleReadMapper. Extended: MqConstants, ComposeMessage.VoiceConfig, ResultCode, RabbitMqConfig (media), VolcanoTtsClient, TtsSynthesisService, ComposeService |
