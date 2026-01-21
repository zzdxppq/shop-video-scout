# Models & Types Cumulative Registry

> Auto-generated on first story creation
> Updated by Dev Agent after each story completion

## Registry Metadata

**Last Updated**: 2026-01-22
**Total Stories Tracked**: 1
**Total Models**: 4
**Repository**: shop-video-scout
**Mode**: monolith

## Java Classes

### Response Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| R<T> | Response Wrapper | common-core/.../result/R.java | 1.1 |
| ResultCode | Enum | common-core/.../result/ResultCode.java | 1.1 |

### Exception Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| BusinessException | Exception | common-core/.../exception/BusinessException.java | 1.1 |
| GlobalExceptionHandler | Handler | common-core/.../exception/GlobalExceptionHandler.java | 1.1 |

### Entity Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| BaseEntity | Abstract Entity | common-mybatis/.../entity/BaseEntity.java | 1.1 |

### Configuration Classes
| Class | Type | File | Story |
|-------|------|------|-------|
| RedisConfig | Config | common-core/.../config/RedisConfig.java | 1.1 |
| MybatisPlusConfig | Config | common-mybatis/.../config/MybatisPlusConfig.java | 1.1 |
| RateLimitConfig | Config | gateway-service/.../config/RateLimitConfig.java | 1.1 |

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

## Naming Conventions & Patterns

| Pattern | Convention |
|---------|------------|
| Class naming | PascalCase |
| Package naming | lowercase |
| Constant naming | UPPER_SNAKE_CASE |

## Models by Story

| Story | Models Added |
|-------|--------------|
| 1.1 | R, ResultCode, BusinessException, GlobalExceptionHandler, BaseEntity, RedisConfig, MybatisPlusConfig, RedisUtils, JwtUtils, JwtProperties, RateLimitConfig |
