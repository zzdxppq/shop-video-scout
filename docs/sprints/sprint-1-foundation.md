# Sprint 1: 基础设施 (Foundation)

## Sprint 信息

| 属性 | 值 |
|------|-----|
| Sprint 编号 | 1 |
| Sprint 名称 | Foundation |
| 状态 | Planned |
| 创建日期 | 2026-01-22 |

## Sprint 目标

搭建开发基础设施和核心服务框架，实现用户认证和前端登录功能。

## 交付物

- 可运行的 CI/CD 流水线
- 后端微服务基础架构 (Spring Cloud)
- 用户认证服务 (JWT)
- 前端项目骨架 (Vue 3 + TypeScript)
- 可登录的完整前后端系统

## Sprint Backlog

### Story 0.1: CI/CD流水线搭建
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | DevOps |
| 依赖 | 无 |
| 文件 | [0.1.cicd-pipeline.md](../stories/0.1.cicd-pipeline.md) |

**Tasks:**
- [ ] T1: GitHub Actions 工作流配置
- [ ] T2: Docker 镜像构建流程
- [ ] T3: 阿里云容器镜像服务集成

---

### Story 1.1: 后端微服务基础架构搭建
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Backend |
| 依赖 | 无 |
| 文件 | [1.1.backend-microservices.md](../stories/1.1.backend-microservices.md) |

**Tasks:**
- [ ] T1: Spring Cloud 项目脚手架
- [ ] T2: API Gateway 配置
- [ ] T3: 服务注册发现 (Nacos)
- [ ] T4: 数据库连接配置 (MySQL + Redis)

---

### Story 1.2: 用户认证服务实现
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Backend |
| 依赖 | 1.1 |
| 文件 | [1.2.user-auth-service.md](../stories/1.2.user-auth-service.md) |

**Tasks:**
- [ ] T1: 微信小程序登录接口
- [ ] T2: JWT Token 生成与验证
- [ ] T3: 用户信息存储

---

### Story 1.3: 前端项目搭建与登录页面
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Frontend |
| 依赖 | 1.2 |
| 文件 | [1.3.frontend-login.md](../stories/1.3.frontend-login.md) |

**Tasks:**
- [ ] T1: Vue 3 + Vite 项目初始化
- [ ] T2: 路由和状态管理配置
- [ ] T3: 微信登录组件
- [ ] T4: Token 存储和刷新逻辑

---

## 依赖关系

```
[0.1 CI/CD] (并行)

[1.1 后端微服务] ──► [1.2 用户认证] ──► [1.3 前端登录]
```

## 验收标准

- [ ] CI/CD 流水线可自动构建和部署
- [ ] 后端服务可启动并通过健康检查
- [ ] 用户可通过微信登录获取 JWT Token
- [ ] 前端可正确存储 Token 并保持登录状态

## 风险与阻塞

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 微信小程序审核延迟 | 登录功能无法测试 | 先使用 Mock 登录 |
| 阿里云服务配置 | 部署延迟 | 提前申请资源 |

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-22 | SM Agent | Sprint 创建 |
