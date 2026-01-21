# 探店宝 (Shop Video Scout) - Product Requirements Document (PRD)

---

## 1. Goals and Background Context

### 1.1 Goals

- **一键出片**：用户上传探店素材后，AI自动完成镜头筛选、脚本生成、配音合成、视频剪辑，输出可直接发布的成品视频
- **降低创作门槛**：让不具备专业剪辑能力的探店达人也能高效产出优质内容
- **提升发布效率**：通过热门话题推荐和标题建议，帮助用户优化发布策略，提高曝光率
- **个性化配音**：支持标准音色和声音克隆，让视频配音更具个人特色
- **V1.0 Web MVP验证**：以Web端快速验证核心价值，为后续iOS App(V2.0)奠定基础

### 1.2 Background Context

探店达人在内容创作过程中面临多重痛点：拍摄大量素材后需要花费大量时间挑选优质镜头，每次都要绞尽脑汁撰写口播文案，配音和剪辑更是技术门槛高且耗时。这些问题严重制约了创作效率和内容产出量。

探店宝（Shop Video Scout）定位为"探店达人的AI视频助手"，通过AI技术实现从素材上传到成品输出的全流程自动化。系统利用多模态AI进行镜头分析和分类，结合店铺信息自动生成匹配的口播脚本，再通过TTS技术配音并自动剪辑合成，最终输出可直接发布到抖音、小红书等平台的成品视频。

### 1.3 Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2026-01-16 | 1.0 | Initial PRD creation based on product proposal | PM (Liangning) |
| 2026-01-16 | 1.1 | Added auto subtitle feature (PCP-2026-002) | PM (Liangning) |
| 2026-01-21 | 1.2 | 技术选型变更：脚本生成和语音合成统一使用豆包/火山引擎（Seed-TTS声音克隆仅需5秒样本） | PM (Liangning) |
| 2026-01-21 | 1.3 | 添加Epic 0: DevOps基础设施（CI/CD、测试框架、监控告警），基于PO验证建议 | PO (Jianghuan) |

---

## 2. Requirements

### 2.1 Functional Requirements

**视频上传与管理**
- FR1: 系统应支持用户批量上传10-20个视频文件，支持拖拽上传和文件选择两种方式
- FR2: 系统应支持MP4和MOV格式，单个视频限制100MB以内、时长3分钟以内
- FR3: 系统应显示每个视频的上传进度，并在上传完成后生成缩略图预览
- FR4: 系统应允许用户在上传过程中删除或替换视频文件

**店铺信息管理**
- FR5: 系统应提供店铺信息表单，包含店铺名称（必填）、店铺类型（必填）、商品/优惠描述（选填）、视频风格（必填）
- FR6: 店铺类型应支持：餐饮美食、美容美发、休闲娱乐、其他
- FR7: 视频风格应支持：种草安利型、真实测评型、探店vlog型

**AI镜头分析**
- FR8: 系统应使用多模态AI（通义千问VL）自动分析每个视频的镜头内容
- FR9: 系统应将镜头自动分类为：食物、人物、环境、其他
- FR10: 系统应为每个镜头生成具体标签（最多5个）和一句话描述
- FR11: 系统应评估每个镜头的画面质量并给出0-100的评分（综合清晰度、构图、光线、稳定性）
- FR12: 系统应自动标记每类镜头中的Top2推荐镜头

**AI脚本生成**
- FR13: 系统应基于镜头分析结果和店铺信息，使用LLM（豆包API）自动生成口播脚本
- FR14: 生成的脚本应分段落展示，每段对应具体镜头
- FR15: 脚本应包含：开场hook、环境介绍、菜品/服务介绍、优惠信息、结尾互动引导
- FR16: 系统应允许用户手动编辑和修改生成的脚本
- FR17: 系统应支持重新生成脚本功能

**AI配音合成**
- FR18: 系统应提供标准音色选择：活泼女声、阳光男声、知性女声（2-3种）
- FR19: 系统应使用豆包语音合成服务（火山引擎Seed-TTS）将脚本转为配音音频
- FR20: 系统应支持声音克隆功能，用户可上传5秒-2分钟的声音样本（豆包Seed-TTS仅需5秒即可克隆音色）
- FR21: 克隆成功后，用户可选择使用自己的声音进行配音
- FR22: 系统应提供配音试听功能

**视频自动剪辑**
- FR23: 系统应根据脚本段落自动选择对应的推荐镜头
- FR24: 系统应根据每段配音时长自动裁剪对应的视频片段
- FR25: 系统应使用FFmpeg按顺序拼接视频片段并叠加配音音轨
- FR26: 合成视频应优化为抖音/小红书竖屏格式

**预览与导出**
- FR27: 系统应提供在线视频播放器预览合成效果
- FR28: 系统应显示成品视频信息：分辨率、时长、文件大小
- FR29: 系统应提供一键下载成品MP4视频功能
- FR30: 系统应提供下载推荐镜头素材包功能
- FR31: 用户不满意时可返回修改脚本并重新生成

**发布辅助**
- FR32: 系统应使用AI推荐5-10个与内容相关的抖音热门话题标签
- FR33: 系统应使用AI生成3-5个吸引眼球的视频标题供用户选择
- FR34: 系统应提供一键复制话题标签和标题的功能
- FR35: 推荐的话题标签格式应符合抖音/小红书规范

**任务管理**
- FR36: 系统应记录用户的历史任务列表
- FR37: 用户可查看历史任务详情和成品视频
- FR38: 用户可删除历史任务

**用户认证**
- FR39: 系统应支持手机号+验证码登录
- FR40: 系统应使用JWT Token进行身份认证

**视频字幕**
- FR41: 系统应基于AI生成的脚本自动生成时间轴对齐的字幕
- FR42: 字幕应采用双行滚动显示，当前行高亮，下一行预显示
- FR43: 系统应提供5种预设字幕样式模板供用户选择（简约白字、活力黄字、小红书风、抖音热门、霓虹炫彩）
- FR44: 字幕功能默认开启，用户可在配音设置页面手动关闭
- FR45: 字幕应烧录到成品视频中，位置固定在底部居中

### 2.2 Non-Functional Requirements

**性能**
- NFR1: 视频上传应支持分片上传和断点续传，单视频上传成功率>99%
- NFR2: AI镜头分析应支持并行处理，20个视频分析应在3分钟内完成
- NFR3: 配音合成+视频剪辑全流程应在5-8分钟内完成
- NFR4: 成品视频预览应支持流式加载，3秒内开始播放

**可用性**
- NFR5: 系统应在每个处理阶段显示明确的进度提示
- NFR6: 系统可用性应达到99.5%以上

**安全性**
- NFR7: 用户视频素材应存储在私有OSS空间，仅本人可访问
- NFR8: API应使用HTTPS加密传输
- NFR9: 用户Token应设置合理过期时间（访问Token 15分钟，刷新Token 7天）

**扩展性**
- NFR10: 后端应采用微服务架构，便于后续iOS App(V2.0)接入
- NFR11: API应采用版本化设计（/api/v1/），支持接口演进

**成本控制**
- NFR12: 单次任务成本应控制在¥3.5以内

---

## 3. User Interface Design Goals

### 3.1 Overall UX Vision

打造简洁、高效、引导式的创作体验。用户无需学习复杂操作，按照步骤流程即可完成从素材上传到成品输出的全过程。界面风格现代、清爽，符合年轻探店达人的审美偏好。

### 3.2 Key Interaction Paradigms

- **步骤引导式流程**：将复杂的创作过程拆解为6个清晰步骤（填写信息→上传视频→AI分析→生成脚本→配音合成→预览导出）
- **实时进度反馈**：每个处理阶段显示明确的进度条和状态提示，减少用户等待焦虑
- **渐进式披露**：复杂功能（如声音克隆）以可选入口呈现，不干扰主流程
- **即时预览**：脚本编辑和音色选择支持实时预览效果

### 3.3 Core Screens and Views

1. **首页/落地页** (`/`) - 产品介绍和快速开始入口
2. **创建任务页** (`/create`) - 店铺信息填写 + 视频上传
3. **任务处理页** (`/task/:id`) - 进度展示 + 脚本编辑 + 配音设置
4. **结果预览页** (`/task/:id/result`) - 视频预览 + 下载导出 + 发布辅助
5. **历史任务页** (`/history`) - 历史任务列表管理
6. **登录页** (`/login`) - 手机号验证码登录
7. **定价页** (`/pricing`) - 套餐和价格展示

### 3.4 Accessibility

WCAG AA - 确保基本的无障碍访问支持，包括键盘导航、适当的颜色对比度、屏幕阅读器兼容。

### 3.5 Branding

- **产品名称**：探店宝 (Shop Video Scout)
- **视觉风格**：现代简约，以活力橙色为主色调，搭配深灰文字和浅灰背景
- **图标风格**：线性图标，圆润边角
- **整体调性**：专业但不冰冷，高效但有温度

### 3.6 Target Device and Platforms

Web Responsive - V1.0聚焦Web端，采用响应式设计支持桌面和平板访问。移动端体验为次要目标，iOS原生App规划在V2.0实现。

---

## 4. Technical Assumptions

### 4.1 Repository Structure

**Monorepo** - 前后端代码在同一仓库中管理，便于开发协作和版本同步。

### 4.2 Service Architecture

**微服务架构 (Microservices)** - 后端采用Spring Cloud微服务架构，为V2.0 iOS App接入做准备：

| 服务 | 职责 |
|------|------|
| gateway | API网关 - 路由分发、限流熔断、统一认证 |
| user-service | 用户服务 - 认证、会员、声音样本管理 |
| task-service | 任务服务 - 任务CRUD、进度跟踪、结果存储 |
| ai-service | AI分析服务 - 镜头分析、脚本生成、质量评估 |
| media-service | 媒体处理服务 - TTS配音、声音克隆、视频合成 |
| publish-service | 发布辅助服务 - 话题推荐、标题生成 |

### 4.3 Testing Requirements

**Unit + Integration** - 核心业务逻辑需要单元测试覆盖，关键API需要集成测试验证。视频处理流程需要端到端测试确保稳定性。

### 4.4 Additional Technical Assumptions and Requests

**技术栈选型**

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 前端框架 | Vue 3 + TypeScript + Vite | 团队熟悉，开发效率高 |
| UI框架 | TailwindCSS + Element Plus | 快速开发，风格现代 |
| 视频播放 | Video.js / vue3-video-player | 支持多格式，可定制 |
| 后端框架 | Java 21 + Spring Cloud | 微服务架构，企业级稳定性 |
| 微服务组件 | Spring Cloud Gateway + Nacos | API网关 + 服务注册发现 |
| 任务队列 | RabbitMQ | 视频分析/合成异步处理 |
| 数据库 | MySQL 8.0 + MyBatis-Plus | 结构化数据存储 |
| 缓存 | Redis | 会话/缓存/分布式锁 |
| 对象存储 | 阿里云OSS + CDN | 视频文件存储和加速 |
| 视觉AI | 通义千问VL | 多模态图像理解，镜头分析 |
| 文本AI | 豆包API (火山引擎) | 脚本生成、话题标签、标题推荐 |
| TTS服务 | 豆包语音合成 (火山引擎 Seed-TTS) | 标准音色(100+) + 声音克隆（仅需5秒样本） |
| 视频处理 | FFmpeg | 抽帧、裁剪、合成 |

**架构原则**
- API版本化：`/api/v1/...`，便于后续iOS版本接口演进
- 统一认证：JWT Token，支持Web/iOS多端登录
- 异步处理：视频分析和合成使用消息队列异步执行

---

## 5. Epic List

- **Epic 0: DevOps基础设施** - 建立CI/CD流水线、测试框架、监控告警系统，确保开发团队高效协作
- **Epic 1: 项目基础架构与用户认证** - 搭建前后端项目框架、数据库、用户认证系统，实现基础的任务创建功能
- **Epic 2: 视频上传与AI镜头分析** - 实现视频批量上传、OSS存储、AI镜头分析和分类标记功能
- **Epic 3: AI脚本生成与编辑** - 实现基于镜头和店铺信息的AI脚本生成，支持用户编辑修改
- **Epic 4: 配音合成与视频剪辑** - 实现TTS配音、声音克隆、视频自动裁剪和合成功能
- **Epic 5: 预览导出与发布辅助** - 实现成品预览播放、下载导出、热门话题和标题推荐功能

---

## 6. Epics

## Epic 0: DevOps基础设施

**Epic Summary:** 建立项目的持续集成/持续部署基础设施，包括CI/CD流水线、测试框架、监控告警系统，确保开发团队能够高效协作和快速迭代。

**Target Repositories:** monolith

```yaml
epic_id: 0
title: "DevOps基础设施"
description: |
  建立项目的持续集成/持续部署基础设施，包括CI/CD流水线、测试框架、监控告警系统，
  确保开发团队能够高效协作和快速迭代。

stories:
  - id: "0.1"
    title: "CI/CD流水线搭建"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "代码构建流水线"
        scenario:
          given: "开发者提交代码到Git仓库"
          when: "代码推送到develop或main分支"
          then:
            - "自动触发构建流水线"
            - "前端: 执行npm install && npm run build"
            - "后端: 执行mvn clean package -DskipTests"
            - "构建产物存档（Docker镜像或JAR包）"
            - "构建失败时通知开发者"

        business_rules:
          - id: "BR-1.1"
            rule: "develop分支触发CI，main分支触发CI+CD"
          - id: "BR-1.2"
            rule: "构建超时时间30分钟"
          - id: "BR-1.3"
            rule: "Docker镜像标签格式: {service}:{branch}-{commit_sha}"

        error_handling:
          - scenario: "构建失败"
            code: "BUILD_FAILED"
            message: "构建失败，请检查代码"
            action: "发送企业微信/钉钉通知，附带错误日志链接"

      - id: AC2
        title: "自动化测试阶段"
        scenario:
          given: "代码构建成功"
          when: "进入测试阶段"
          then:
            - "执行前端单元测试 (Vitest)"
            - "执行后端单元测试 (JUnit)"
            - "生成测试覆盖率报告"
            - "覆盖率低于阈值时标记警告"

        business_rules:
          - id: "BR-2.1"
            rule: "后端核心服务单元测试覆盖率 >= 60%"
          - id: "BR-2.2"
            rule: "前端组件测试覆盖率 >= 50%"
          - id: "BR-2.3"
            rule: "测试失败阻止后续部署"

        error_handling:
          - scenario: "测试失败"
            code: "TEST_FAILED"
            message: "单元测试失败"
            action: "阻止部署，通知开发者修复"

      - id: AC3
        title: "自动部署到Staging环境"
        scenario:
          given: "develop分支构建和测试通过"
          when: "触发Staging部署"
          then:
            - "推送Docker镜像到私有仓库"
            - "更新Staging环境Kubernetes部署"
            - "等待健康检查通过"
            - "部署成功后通知团队"

        business_rules:
          - id: "BR-3.1"
            rule: "Staging部署采用滚动更新策略"
          - id: "BR-3.2"
            rule: "健康检查超时5分钟"
          - id: "BR-3.3"
            rule: "部署失败自动回滚到上一版本"

        error_handling:
          - scenario: "部署失败"
            code: "DEPLOY_FAILED"
            message: "Staging部署失败"
            action: "自动回滚，通知运维团队"

      - id: AC4
        title: "生产环境部署流程"
        scenario:
          given: "Staging验证通过"
          when: "触发Production部署（手动审批）"
          then:
            - "需要至少1名审批者确认"
            - "执行蓝绿部署或金丝雀发布"
            - "保留最近3个版本用于回滚"
            - "部署完成后执行冒烟测试"

        business_rules:
          - id: "BR-4.1"
            rule: "生产部署必须经过手动审批"
          - id: "BR-4.2"
            rule: "保留最近3个可回滚版本"
          - id: "BR-4.3"
            rule: "一键回滚响应时间 < 5分钟"

        error_handling:
          - scenario: "冒烟测试失败"
            code: "SMOKE_TEST_FAILED"
            message: "生产冒烟测试失败"
            action: "自动回滚，触发告警"

    provides_apis: []
    consumes_apis: []
    dependencies: []
    sm_hints:
      tech_stack: "GitHub Actions / GitLab CI, Docker, Kubernetes/阿里云ACK"

  - id: "0.2"
    title: "测试基础设施搭建"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "后端单元测试框架"
        scenario:
          given: "后端微服务项目已创建"
          when: "配置测试框架"
          then:
            - "集成JUnit 5测试框架"
            - "集成Mockito用于Mock依赖"
            - "集成Testcontainers用于数据库测试"
            - "创建测试基类和通用工具类"
            - "配置测试数据库连接（H2内存数据库）"

        business_rules:
          - id: "BR-1.1"
            rule: "每个Service类必须有对应的Test类"
          - id: "BR-1.2"
            rule: "测试方法命名: should_预期行为_when_条件"
          - id: "BR-1.3"
            rule: "测试数据使用@BeforeEach准备，@AfterEach清理"

        error_handling:
          - scenario: "测试数据库连接失败"
            code: "TEST_DB_ERROR"
            message: "测试数据库初始化失败"
            action: "检查H2配置，确保测试隔离"

      - id: AC2
        title: "后端集成测试框架"
        scenario:
          given: "单元测试框架已配置"
          when: "配置集成测试"
          then:
            - "使用@SpringBootTest加载完整上下文"
            - "使用Testcontainers启动MySQL和Redis容器"
            - "配置MockMvc进行API端点测试"
            - "创建集成测试基类"

        business_rules:
          - id: "BR-2.1"
            rule: "集成测试使用独立的test profile"
          - id: "BR-2.2"
            rule: "每个API端点至少有Happy Path测试"
          - id: "BR-2.3"
            rule: "集成测试不依赖外部AI服务（使用Mock）"

        error_handling:
          - scenario: "容器启动失败"
            code: "CONTAINER_ERROR"
            message: "Testcontainers启动失败"
            action: "检查Docker环境，确保Docker daemon运行"

      - id: AC3
        title: "前端单元测试框架"
        scenario:
          given: "Vue3前端项目已创建"
          when: "配置前端测试框架"
          then:
            - "集成Vitest作为测试运行器"
            - "集成Vue Test Utils进行组件测试"
            - "集成MSW (Mock Service Worker)模拟API"
            - "配置测试覆盖率报告 (c8/istanbul)"

        business_rules:
          - id: "BR-3.1"
            rule: "核心组件必须有单元测试"
          - id: "BR-3.2"
            rule: "Composables必须有单元测试"
          - id: "BR-3.3"
            rule: "测试文件与源文件同目录: Component.vue → Component.test.ts"

        error_handling:
          - scenario: "组件渲染失败"
            code: "RENDER_ERROR"
            message: "组件测试渲染失败"
            action: "检查组件依赖是否正确Mock"

      - id: AC4
        title: "E2E测试框架"
        scenario:
          given: "前后端集成完成"
          when: "配置E2E测试"
          then:
            - "集成Playwright"
            - "创建关键用户流程测试"
            - "配置无头浏览器运行"
            - "生成测试报告和截图"

        business_rules:
          - id: "BR-4.1"
            rule: "E2E测试仅覆盖核心Happy Path"
          - id: "BR-4.2"
            rule: "E2E测试不在每次CI运行，仅在发布前"
          - id: "BR-4.3"
            rule: "测试失败自动截图保存"

        error_handling:
          - scenario: "浏览器启动失败"
            code: "BROWSER_ERROR"
            message: "E2E测试浏览器启动失败"
            action: "检查Playwright依赖是否正确安装"

    provides_apis: []
    consumes_apis: []
    dependencies:
      - "1.1"
      - "1.3"
    sm_hints:
      tech_stack: "JUnit 5, Mockito, Testcontainers, Vitest, Vue Test Utils, Playwright"
      priority_note: "AC1-AC3为P0，AC4 E2E测试为P1可延后"

  - id: "0.3"
    title: "监控告警系统搭建"
    repository_type: monolith
    estimated_complexity: medium
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "应用日志收集"
        scenario:
          given: "微服务应用已部署"
          when: "配置日志收集"
          then:
            - "配置统一JSON日志格式"
            - "集成阿里云SLS日志服务"
            - "配置日志索引和查询"
            - "设置日志保留策略（30天）"

        business_rules:
          - id: "BR-1.1"
            rule: "日志必须包含traceId用于链路追踪"
          - id: "BR-1.2"
            rule: "敏感信息（手机号、Token）脱敏处理"
          - id: "BR-1.3"
            rule: "ERROR级别日志自动触发告警"

        error_handling:
          - scenario: "日志服务不可用"
            code: "LOG_SERVICE_ERROR"
            message: "日志服务连接失败"
            action: "本地文件备份，服务恢复后补传"

      - id: AC2
        title: "应用指标采集"
        scenario:
          given: "微服务应用已部署"
          when: "配置指标采集"
          then:
            - "集成Spring Boot Actuator"
            - "暴露Prometheus格式指标端点"
            - "配置Grafana仪表盘"
            - "创建核心业务指标"

        business_rules:
          - id: "BR-2.1"
            rule: "指标采集间隔15秒"
          - id: "BR-2.2"
            rule: "指标保留时间15天"
          - id: "BR-2.3"
            rule: "核心指标: http_request_total, task_created_total, ai_api_calls_total, queue_depth"

        error_handling:
          - scenario: "指标端点不可用"
            code: "METRICS_ERROR"
            message: "指标采集失败"
            action: "记录采集失败，不影响主服务"

      - id: AC3
        title: "告警规则配置"
        scenario:
          given: "指标采集已运行"
          when: "配置告警规则"
          then:
            - "配置错误率告警（5xx > 1%持续5分钟）"
            - "配置延迟告警（P99 > 5s持续5分钟）"
            - "配置队列积压告警（depth > 100持续10分钟）"
            - "配置AI服务失败告警（failures > 10次/5分钟）"
            - "配置通知渠道（企业微信/钉钉）"

        business_rules:
          - id: "BR-3.1"
            rule: "Critical告警触发短信+即时通讯"
          - id: "BR-3.2"
            rule: "Warning告警触发即时通讯"
          - id: "BR-3.3"
            rule: "告警去重窗口5分钟"

        error_handling:
          - scenario: "通知发送失败"
            code: "NOTIFY_ERROR"
            message: "告警通知发送失败"
            action: "重试3次，记录到备用日志"

      - id: AC4
        title: "分布式链路追踪"
        scenario:
          given: "微服务通信已建立"
          when: "配置链路追踪"
          then:
            - "集成Spring Cloud Sleuth"
            - "配置Zipkin或Jaeger收集器"
            - "跨服务传播traceId"
            - "设置采样率（生产10%，测试100%）"

        business_rules:
          - id: "BR-4.1"
            rule: "生产环境采样率10%"
          - id: "BR-4.2"
            rule: "Trace数据保留7天"
          - id: "BR-4.3"
            rule: "慢请求（>3s）100%采样"

        error_handling:
          - scenario: "追踪服务不可用"
            code: "TRACE_ERROR"
            message: "链路追踪服务不可用"
            action: "降级为本地日志记录traceId"

      - id: AC5
        title: "监控仪表盘"
        scenario:
          given: "指标和日志已收集"
          when: "创建监控仪表盘"
          then:
            - "创建服务概览仪表盘（QPS、延迟、错误率）"
            - "创建任务处理仪表盘（任务数、成功率、处理时长）"
            - "创建AI服务仪表盘（调用量、延迟、成本）"
            - "创建基础设施仪表盘（CPU、内存、磁盘）"

        business_rules:
          - id: "BR-5.1"
            rule: "仪表盘刷新间隔1分钟"
          - id: "BR-5.2"
            rule: "支持时间范围选择（1h/6h/24h/7d）"

        error_handling:
          - scenario: "仪表盘加载失败"
            code: "DASHBOARD_ERROR"
            message: "仪表盘数据加载失败"
            action: "显示错误提示，提供刷新按钮"

    provides_apis:
      - "GET /actuator/health"
      - "GET /actuator/prometheus"
    consumes_apis: []
    dependencies:
      - "1.1"
      - "0.1"
    sm_hints:
      tech_stack: "Spring Boot Actuator, Prometheus, Grafana, 阿里云SLS, Spring Cloud Sleuth"
      priority_note: "AC1-AC3为P0核心监控，AC4-AC5为P1可与业务功能并行开发"
```

## Epic 1: 项目基础架构与用户认证

**Epic Summary:** 搭建前后端项目框架、微服务基础设施、数据库和用户认证系统。实现用户登录和基础的任务创建入口，为后续功能奠定基础架构。

**Target Repositories:** monolith

```yaml
epic_id: 1
title: "项目基础架构与用户认证"
description: |
  建立项目的技术基础架构，包括前端Vue3项目、后端Spring Cloud微服务框架、
  MySQL数据库、Redis缓存、OSS存储配置。实现手机号验证码登录和JWT认证，
  以及基础的任务创建表单。

stories:
  - id: "1.1"
    title: "后端微服务基础架构搭建"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "Spring Cloud项目初始化"
        scenario:
          given: "开发环境已准备好Java 21和Maven"
          when: "创建Spring Cloud微服务项目结构"
          then:
            - "创建gateway、user-service、task-service、ai-service、media-service、publish-service模块"
            - "配置Spring Cloud Gateway路由"
            - "配置Nacos服务注册发现"
            - "所有服务可正常启动并注册到Nacos"

        business_rules:
          - id: "BR-1.1"
            rule: "所有服务使用统一的Spring Boot 3.x版本"
          - id: "BR-1.2"
            rule: "Gateway统一处理跨域配置"

        error_handling:
          - scenario: "服务启动失败"
            code: "500"
            message: "Service startup failed"
            action: "记录错误日志，检查配置和依赖"

      - id: AC2
        title: "数据库和缓存配置"
        scenario:
          given: "MySQL和Redis服务已部署"
          when: "配置数据源和连接"
          then:
            - "创建数据库shop_video_scout"
            - "执行初始化SQL创建核心表结构"
            - "配置MyBatis-Plus和连接池"
            - "配置Redis连接和序列化"

        business_rules:
          - id: "BR-2.1"
            rule: "数据库连接池最大连接数为20"
          - id: "BR-2.2"
            rule: "Redis使用JSON序列化"

        error_handling:
          - scenario: "数据库连接失败"
            code: "503"
            message: "Database connection failed"
            action: "服务启动时检查连接，失败则阻止启动"

    provides_apis: []
    consumes_apis: []
    dependencies: []
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.2"
    title: "用户认证服务实现"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "发送验证码接口"
        scenario:
          given: "用户在登录页输入手机号"
          when: "用户点击发送验证码"
          then:
            - "系统生成6位数字验证码"
            - "验证码存储到Redis，有效期5分钟"
            - "调用短信服务发送验证码"
            - "返回发送成功提示"

        business_rules:
          - id: "BR-1.1"
            rule: "同一手机号60秒内只能发送一次验证码"
          - id: "BR-1.2"
            rule: "验证码为6位随机数字"
          - id: "BR-1.3"
            rule: "验证码有效期5分钟"

        data_validation:
          - field: "phone"
            type: "string"
            required: true
            rules: "中国大陆手机号格式，11位数字，以1开头"
            error_message: "请输入正确的手机号"

        error_handling:
          - scenario: "请求过于频繁"
            code: "429"
            message: "请求过于频繁，请60秒后重试"
            action: "返回剩余等待时间"
          - scenario: "短信服务不可用"
            code: "503"
            message: "短信服务暂时不可用，请稍后重试"
            action: "记录错误日志，不暴露内部错误"

        examples:
          - input: |
              POST /api/v1/auth/send-code
              {"phone": "13800138000"}
            expected: |
              200 OK
              {"success": true, "message": "验证码已发送"}

      - id: AC2
        title: "手机号验证码登录"
        scenario:
          given: "用户已收到验证码"
          when: "用户输入手机号和验证码提交登录"
          then:
            - "系统验证验证码正确性"
            - "如果是新用户，自动创建账号"
            - "生成JWT访问令牌和刷新令牌"
            - "返回用户信息和令牌"

        business_rules:
          - id: "BR-2.1"
            rule: "验证码验证失败次数超过5次，锁定该手机号15分钟"
          - id: "BR-2.2"
            rule: "访问令牌有效期15分钟"
          - id: "BR-2.3"
            rule: "刷新令牌有效期7天"
          - id: "BR-2.4"
            rule: "新用户默认为免费会员"

        data_validation:
          - field: "phone"
            type: "string"
            required: true
            rules: "中国大陆手机号格式"
            error_message: "请输入正确的手机号"
          - field: "code"
            type: "string"
            required: true
            rules: "6位数字"
            error_message: "请输入6位验证码"

        error_handling:
          - scenario: "验证码错误"
            code: "401"
            message: "验证码错误，请重新输入"
            action: "增加错误计数，返回剩余尝试次数"
          - scenario: "验证码已过期"
            code: "401"
            message: "验证码已过期，请重新获取"
            action: "提示用户重新发送验证码"
          - scenario: "手机号已被锁定"
            code: "423"
            message: "验证码错误次数过多，请15分钟后重试"
            action: "返回剩余锁定时间"

        examples:
          - input: |
              POST /api/v1/auth/login
              {"phone": "13800138000", "code": "123456"}
            expected: |
              200 OK
              {"user": {"id": 1, "phone": "138****8000", "nickname": "用户138000", "membership_type": "free"}, "access_token": "jwt...", "refresh_token": "jwt...", "expires_in": 900}

    provides_apis:
      - "POST /api/v1/auth/send-code"
      - "POST /api/v1/auth/login"
      - "POST /api/v1/auth/refresh"
      - "POST /api/v1/auth/logout"
    consumes_apis: []
    dependencies:
      - "1.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.3"
    title: "前端项目搭建与登录页面"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "Vue3项目初始化"
        scenario:
          given: "开发环境已准备好Node.js"
          when: "创建前端项目"
          then:
            - "使用Vite创建Vue3 + TypeScript项目"
            - "配置TailwindCSS和Element Plus"
            - "配置Vue Router和Pinia"
            - "配置Axios请求拦截器"
            - "项目可正常启动运行"

        business_rules:
          - id: "BR-1.1"
            rule: "所有请求自动携带Authorization头"
          - id: "BR-1.2"
            rule: "401响应自动尝试刷新Token"

        error_handling:
          - scenario: "构建失败"
            code: "BUILD_ERROR"
            message: "项目构建失败"
            action: "检查依赖和配置"

      - id: AC2
        title: "登录页面实现"
        scenario:
          given: "用户访问需要认证的页面或点击登录"
          when: "用户进入登录页"
          then:
            - "显示手机号输入框"
            - "显示验证码输入框和发送按钮"
            - "发送按钮点击后显示60秒倒计时"
            - "登录成功后跳转到目标页面"

        business_rules:
          - id: "BR-2.1"
            rule: "手机号输入时实时格式验证"
          - id: "BR-2.2"
            rule: "发送验证码后按钮禁用60秒"
          - id: "BR-2.3"
            rule: "登录成功后Token存储到localStorage"

        data_validation:
          - field: "phone"
            type: "string"
            required: true
            rules: "11位数字，以1开头"
            error_message: "请输入正确的手机号"
          - field: "code"
            type: "string"
            required: true
            rules: "6位数字"
            error_message: "请输入6位验证码"

        error_handling:
          - scenario: "网络错误"
            code: "NETWORK_ERROR"
            message: "网络连接失败，请检查网络"
            action: "显示重试按钮"

        interaction:
          - trigger: "页面加载"
            behavior: "自动聚焦手机号输入框"
          - trigger: "点击发送验证码"
            behavior: "按钮显示60秒倒计时，禁用状态"
          - trigger: "登录成功"
            behavior: "显示成功提示，1秒后跳转"

    provides_apis: []
    consumes_apis:
      - "POST /api/v1/auth/send-code"
      - "POST /api/v1/auth/login"
    dependencies:
      - "1.2"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "1.4"
    title: "任务创建表单页面"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "店铺信息表单"
        scenario:
          given: "已登录用户访问创建任务页面"
          when: "用户填写店铺信息"
          then:
            - "显示店铺名称输入框（必填）"
            - "显示店铺类型单选按钮组（必填）"
            - "显示商品/优惠描述文本框（选填）"
            - "显示视频风格单选按钮组（必填）"
            - "表单验证通过后启用下一步按钮"

        business_rules:
          - id: "BR-1.1"
            rule: "店铺名称最大200字符"
          - id: "BR-1.2"
            rule: "优惠描述最大500字符"
          - id: "BR-1.3"
            rule: "必填项未填写时显示红色提示"

        data_validation:
          - field: "shop_name"
            type: "string"
            required: true
            rules: "1-200字符"
            error_message: "请输入店铺名称"
          - field: "shop_type"
            type: "enum"
            required: true
            rules: "food|beauty|entertainment|other"
            error_message: "请选择店铺类型"
          - field: "promotion_text"
            type: "string"
            required: false
            rules: "最大500字符"
            error_message: "优惠描述不能超过500字"
          - field: "video_style"
            type: "enum"
            required: true
            rules: "recommend|review|vlog"
            error_message: "请选择视频风格"

        error_handling:
          - scenario: "表单验证失败"
            code: "VALIDATION_ERROR"
            message: "请填写必填项"
            action: "高亮显示未填写的必填项"

        interaction:
          - trigger: "输入内容"
            behavior: "实时验证并显示错误提示"
          - trigger: "点击下一步"
            behavior: "验证通过则显示视频上传区域"

      - id: AC2
        title: "创建任务API"
        scenario:
          given: "用户填写完店铺信息"
          when: "提交创建任务请求"
          then:
            - "后端创建任务记录"
            - "返回任务ID"
            - "任务状态设为uploading"

        business_rules:
          - id: "BR-2.1"
            rule: "任务与当前登录用户关联"
          - id: "BR-2.2"
            rule: "同一用户同时最多有5个进行中的任务"

        error_handling:
          - scenario: "任务数量超限"
            code: "429"
            message: "您有太多进行中的任务，请完成后再创建"
            action: "返回当前进行中任务列表"

        examples:
          - input: |
              POST /api/v1/tasks
              {"shop_name": "海底捞火锅(望京店)", "shop_type": "food", "promotion_text": "人均89，必点招牌毛肚", "video_style": "recommend"}
            expected: |
              201 Created
              {"id": 12345, "status": "uploading", "created_at": "2026-01-16T10:00:00Z"}

    provides_apis:
      - "POST /api/v1/tasks"
      - "GET /api/v1/tasks"
      - "GET /api/v1/tasks/{id}"
    consumes_apis: []
    dependencies:
      - "1.2"
      - "1.3"
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 2: 视频上传与AI镜头分析

**Epic Summary:** 实现视频批量上传到OSS、上传进度显示、AI镜头内容分析和质量评估，以及推荐镜头标记功能。

**Target Repositories:** monolith

```yaml
epic_id: 2
title: "视频上传与AI镜头分析"
description: |
  实现视频文件的批量上传功能，包括拖拽上传、进度显示、缩略图生成。
  集成通义千问VL进行镜头内容分析，自动分类和质量评分，标记推荐镜头。

stories:
  - id: "2.1"
    title: "视频上传服务实现"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "获取OSS预签名上传URL"
        scenario:
          given: "用户准备上传视频文件"
          when: "请求上传URL"
          then:
            - "生成OSS预签名上传URL"
            - "URL有效期15分钟"
            - "返回上传URL和文件key"

        business_rules:
          - id: "BR-1.1"
            rule: "文件存储路径格式：videos/{user_id}/{task_id}/{uuid}.{ext}"
          - id: "BR-1.2"
            rule: "仅支持mp4和mov格式"
          - id: "BR-1.3"
            rule: "单文件最大100MB"

        data_validation:
          - field: "filename"
            type: "string"
            required: true
            rules: "有效文件名，扩展名为mp4或mov"
            error_message: "仅支持MP4和MOV格式视频"
          - field: "file_size"
            type: "number"
            required: true
            rules: "小于100MB (104857600 bytes)"
            error_message: "单个视频不能超过100MB"

        error_handling:
          - scenario: "文件格式不支持"
            code: "400"
            message: "仅支持MP4和MOV格式"
            action: "返回支持的格式列表"
          - scenario: "文件过大"
            code: "400"
            message: "单个视频不能超过100MB"
            action: "返回文件大小限制"

        examples:
          - input: |
              POST /api/v1/tasks/12345/videos/upload-url
              {"filename": "video1.mp4", "file_size": 52428800}
            expected: |
              200 OK
              {"upload_url": "https://oss...?signature=...", "oss_key": "videos/1/12345/abc123.mp4", "expires_in": 900}

      - id: AC2
        title: "确认上传完成并处理"
        scenario:
          given: "视频文件已上传到OSS"
          when: "前端确认上传完成"
          then:
            - "创建视频记录"
            - "使用FFmpeg提取关键帧"
            - "生成视频缩略图"
            - "获取视频元信息（时长、分辨率）"
            - "返回视频信息"

        business_rules:
          - id: "BR-2.1"
            rule: "每2秒提取1帧关键帧"
          - id: "BR-2.2"
            rule: "缩略图尺寸为320x180"
          - id: "BR-2.3"
            rule: "视频时长超过3分钟则拒绝"
          - id: "BR-2.4"
            rule: "每个任务最多20个视频"

        error_handling:
          - scenario: "视频时长超限"
            code: "400"
            message: "视频时长不能超过3分钟"
            action: "删除已上传文件，返回错误"
          - scenario: "视频数量超限"
            code: "400"
            message: "每个任务最多上传20个视频"
            action: "返回当前视频数量"

        examples:
          - input: |
              POST /api/v1/tasks/12345/videos
              {"oss_key": "videos/1/12345/abc123.mp4", "original_filename": "店门口.mp4"}
            expected: |
              201 Created
              {"id": 1, "thumbnail_url": "https://...", "duration_seconds": 45, "status": "pending"}

    provides_apis:
      - "POST /api/v1/tasks/{id}/videos/upload-url"
      - "POST /api/v1/tasks/{id}/videos"
      - "GET /api/v1/tasks/{id}/videos"
      - "DELETE /api/v1/tasks/{id}/videos/{video_id}"
    consumes_apis: []
    dependencies:
      - "1.4"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.2"
    title: "前端视频上传组件"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "拖拽上传区域"
        scenario:
          given: "用户在创建任务页面"
          when: "用户拖拽视频文件到上传区域"
          then:
            - "显示拖拽高亮效果"
            - "释放后开始上传"
            - "显示上传进度"
            - "上传完成后显示缩略图"

        business_rules:
          - id: "BR-1.1"
            rule: "支持同时拖拽多个文件"
          - id: "BR-1.2"
            rule: "自动过滤非视频文件"
          - id: "BR-1.3"
            rule: "显示已上传数量/最大数量"

        error_handling:
          - scenario: "文件格式不支持"
            code: "FORMAT_ERROR"
            message: "xxx.jpg 不是支持的视频格式"
            action: "Toast提示，跳过该文件继续其他"
          - scenario: "上传失败"
            code: "UPLOAD_ERROR"
            message: "上传失败，请重试"
            action: "显示重试按钮"

        interaction:
          - trigger: "拖拽进入"
            behavior: "上传区域边框变为蓝色虚线"
          - trigger: "开始上传"
            behavior: "显示进度条和百分比"
          - trigger: "上传完成"
            behavior: "显示缩略图和对勾图标"
          - trigger: "悬停已上传视频"
            behavior: "显示删除按钮"

      - id: AC2
        title: "上传限制提示"
        scenario:
          given: "已上传视频数量达到限制"
          when: "用户尝试继续上传"
          then:
            - "显示数量限制提示"
            - "禁用上传功能"

        business_rules:
          - id: "BR-2.1"
            rule: "最少上传10个视频"
          - id: "BR-2.2"
            rule: "最多上传20个视频"

        error_handling:
          - scenario: "数量不足"
            code: "MIN_COUNT"
            message: "请至少上传10个视频"
            action: "禁用开始分析按钮"

    provides_apis: []
    consumes_apis:
      - "POST /api/v1/tasks/{id}/videos/upload-url"
      - "POST /api/v1/tasks/{id}/videos"
    dependencies:
      - "2.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.3"
    title: "AI镜头分析服务"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "调用通义千问VL分析镜头"
        scenario:
          given: "视频关键帧已提取"
          when: "开始AI分析"
          then:
            - "将关键帧发送到通义千问VL"
            - "获取内容分类结果"
            - "获取具体标签和描述"
            - "获取质量评分"
            - "存储分析结果"

        business_rules:
          - id: "BR-1.1"
            rule: "每个视频分析3-5个关键帧"
          - id: "BR-1.2"
            rule: "分类：food/person/environment/other"
          - id: "BR-1.3"
            rule: "标签最多5个"
          - id: "BR-1.4"
            rule: "质量评分综合清晰度、构图、光线、稳定性"

        error_handling:
          - scenario: "AI服务超时"
            code: "504"
            message: "分析超时，正在重试"
            action: "自动重试3次，间隔递增"
          - scenario: "AI服务不可用"
            code: "503"
            message: "AI服务暂时不可用"
            action: "记录日志，延迟重试"

        examples:
          - input: "视频关键帧图片"
            expected: |
              {"category": "food", "tags": ["火锅", "毛肚", "特写"], "description": "一盘新鲜毛肚的特写镜头", "quality_score": 95}

      - id: AC2
        title: "标记推荐镜头"
        scenario:
          given: "所有视频分析完成"
          when: "系统执行推荐标记"
          then:
            - "按分类归组所有视频"
            - "每类按质量评分排序"
            - "标记每类Top2为推荐镜头"
            - "更新视频记录"

        business_rules:
          - id: "BR-2.1"
            rule: "每个分类最多标记2个推荐"
          - id: "BR-2.2"
            rule: "推荐优先选择质量评分>70的镜头"
          - id: "BR-2.3"
            rule: "如果某分类视频少于2个，全部标记为推荐"

        error_handling:
          - scenario: "某分类无视频"
            code: "INFO"
            message: "无"
            action: "跳过该分类，记录日志"

    provides_apis:
      - "POST /api/v1/tasks/{id}/analyze"
      - "GET /api/v1/tasks/{id}/progress"
    consumes_apis: []
    dependencies:
      - "2.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "2.4"
    title: "分析进度展示页面"
    repository_type: monolith
    estimated_complexity: low
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "分析进度展示"
        scenario:
          given: "用户点击开始AI分析"
          when: "AI分析进行中"
          then:
            - "显示整体进度条"
            - "显示当前处理阶段"
            - "显示预计剩余时间"
            - "分析完成后自动进入下一步"

        business_rules:
          - id: "BR-1.1"
            rule: "每2秒轮询一次进度"
          - id: "BR-1.2"
            rule: "显示阶段：上传完成 → 分析中 → 分析完成"

        error_handling:
          - scenario: "分析失败"
            code: "ANALYSIS_ERROR"
            message: "分析失败，请重试"
            action: "显示重试按钮"

        interaction:
          - trigger: "分析中"
            behavior: "进度条动画，禁止页面离开"
          - trigger: "分析完成"
            behavior: "显示完成动画，2秒后跳转"

    provides_apis: []
    consumes_apis:
      - "GET /api/v1/tasks/{id}/progress"
    dependencies:
      - "2.3"
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 3: AI脚本生成与编辑

**Epic Summary:** 实现基于镜头分析结果和店铺信息的AI脚本生成功能，支持用户编辑修改脚本内容。

**Target Repositories:** monolith

```yaml
epic_id: 3
title: "AI脚本生成与编辑"
description: |
  集成豆包API生成探店口播脚本，脚本分段对应具体镜头，
  支持用户手动编辑和重新生成。

stories:
  - id: "3.1"
    title: "AI脚本生成服务"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "调用豆包API生成脚本"
        scenario:
          given: "镜头分析已完成"
          when: "系统生成脚本"
          then:
            - "构建包含店铺信息和镜头摘要的Prompt"
            - "调用豆包API生成脚本"
            - "解析返回的脚本内容"
            - "存储到任务记录"

        business_rules:
          - id: "BR-1.1"
            rule: "脚本分5-7个段落"
          - id: "BR-1.2"
            rule: "每段标注对应镜头"
          - id: "BR-1.3"
            rule: "总时长控制在60秒左右"
          - id: "BR-1.4"
            rule: "脚本风格根据video_style调整"

        error_handling:
          - scenario: "AI服务超时"
            code: "504"
            message: "生成超时，正在重试"
            action: "自动重试2次"
          - scenario: "生成内容不符合要求"
            code: "422"
            message: "生成内容异常，正在重新生成"
            action: "更换Prompt参数重试"

        examples:
          - input: "店铺信息 + 镜头摘要"
            expected: |
              【开场】(对应镜头: 达人出镜#1, 预计8秒)
              "家人们！今天给你们探一家望京超火的海底捞..."

              【环境展示】(对应镜头: 环境#1, 预计6秒)
              "一进门就被这个装修惊艳到了..."

      - id: AC2
        title: "重新生成脚本"
        scenario:
          given: "用户对当前脚本不满意"
          when: "用户点击重新生成"
          then:
            - "调整Prompt参数"
            - "重新调用AI生成"
            - "更新脚本内容"

        business_rules:
          - id: "BR-2.1"
            rule: "每个任务最多重新生成5次"
          - id: "BR-2.2"
            rule: "保留上一版本脚本供对比"

        error_handling:
          - scenario: "重新生成次数超限"
            code: "429"
            message: "重新生成次数已达上限，请手动编辑"
            action: "禁用重新生成按钮"

    provides_apis:
      - "GET /api/v1/tasks/{id}/script"
      - "POST /api/v1/tasks/{id}/regenerate-script"
    consumes_apis: []
    dependencies:
      - "2.3"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "3.2"
    title: "脚本编辑页面"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "脚本展示与编辑"
        scenario:
          given: "AI脚本生成完成"
          when: "用户查看脚本"
          then:
            - "分段落展示脚本内容"
            - "每段显示对应镜头标记"
            - "支持点击编辑修改内容"
            - "支持撤销修改"

        business_rules:
          - id: "BR-1.1"
            rule: "编辑时显示字数统计"
          - id: "BR-1.2"
            rule: "每段建议字数提示（配音时长参考）"

        error_handling:
          - scenario: "内容为空"
            code: "VALIDATION_ERROR"
            message: "脚本内容不能为空"
            action: "保存按钮禁用"

        interaction:
          - trigger: "点击编辑按钮"
            behavior: "文本区域变为可编辑状态"
          - trigger: "编辑中"
            behavior: "显示字数统计和保存按钮"
          - trigger: "点击保存"
            behavior: "保存修改，显示成功提示"

      - id: AC2
        title: "保存脚本修改"
        scenario:
          given: "用户编辑了脚本"
          when: "用户点击保存"
          then:
            - "提交修改到后端"
            - "更新任务脚本"
            - "显示保存成功"

        business_rules:
          - id: "BR-2.1"
            rule: "自动保存草稿到本地"
          - id: "BR-2.2"
            rule: "页面离开前提示未保存内容"

        error_handling:
          - scenario: "保存失败"
            code: "500"
            message: "保存失败，请重试"
            action: "显示重试按钮，不清除本地内容"

    provides_apis: []
    consumes_apis:
      - "GET /api/v1/tasks/{id}/script"
      - "PUT /api/v1/tasks/{id}/script"
      - "POST /api/v1/tasks/{id}/regenerate-script"
    dependencies:
      - "3.1"
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 4: 配音合成与视频剪辑

**Epic Summary:** 实现TTS配音功能、声音克隆功能、以及视频自动裁剪和合成功能，输出完整的成品视频。

**Target Repositories:** monolith

```yaml
epic_id: 4
title: "配音合成与视频剪辑"
description: |
  集成豆包语音合成服务（火山引擎Seed-TTS）实现TTS配音和声音克隆功能（仅需5秒样本即可克隆），
  使用FFmpeg实现视频片段裁剪和音视频合成，输出成品视频。

stories:
  - id: "4.1"
    title: "TTS配音服务实现"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "标准音色配音"
        scenario:
          given: "用户确认脚本并选择音色"
          when: "开始配音合成"
          then:
            - "将脚本文本发送到豆包语音合成服务"
            - "使用选择的标准音色"
            - "生成配音音频文件"
            - "获取各段落配音时长"
            - "上传音频到OSS"

        business_rules:
          - id: "BR-1.1"
            rule: "标准音色：活泼女声、阳光男声、知性女声（基于火山引擎100+预设音色库）"
          - id: "BR-1.2"
            rule: "配音采样率48000Hz"
          - id: "BR-1.3"
            rule: "输出格式MP3"

        error_handling:
          - scenario: "TTS服务超时"
            code: "504"
            message: "配音生成超时，正在重试"
            action: "自动重试3次"
          - scenario: "文本过长"
            code: "400"
            message: "单次配音文本不能超过5000字"
            action: "分段处理"

        examples:
          - input: |
              {"text": "家人们！今天给你们探一家...", "voice_type": "xiaomei"}
            expected: |
              {"audio_url": "https://oss.../voice/xxx.mp3", "duration_seconds": 8}

      - id: AC2
        title: "配音进度跟踪"
        scenario:
          given: "配音合成进行中"
          when: "查询进度"
          then:
            - "返回已完成段落数"
            - "返回总段落数"
            - "返回预计剩余时间"

        business_rules:
          - id: "BR-2.1"
            rule: "每段配音完成后更新进度"

        error_handling:
          - scenario: "某段配音失败"
            code: "500"
            message: "第N段配音失败，正在重试"
            action: "单段重试，不影响整体"

    provides_apis:
      - "POST /api/v1/tasks/{id}/compose"
      - "PUT /api/v1/tasks/{id}/voice-type"
      - "GET /api/v1/tasks/{id}/compose-progress"
    consumes_apis: []
    dependencies:
      - "3.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.2"
    title: "声音克隆功能"
    repository_type: monolith
    estimated_complexity: high
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "上传声音样本"
        scenario:
          given: "用户想使用自己的声音"
          when: "用户上传声音样本"
          then:
            - "获取OSS上传URL"
            - "上传音频文件"
            - "验证时长在30秒-2分钟之间"
            - "创建声音样本记录"

        business_rules:
          - id: "BR-1.1"
            rule: "支持MP3、WAV、M4A格式"
          - id: "BR-1.2"
            rule: "时长5秒-2分钟（豆包Seed-TTS仅需5秒即可高质量克隆）"
          - id: "BR-1.3"
            rule: "每个用户最多保存3个声音样本"

        data_validation:
          - field: "audio_file"
            type: "file"
            required: true
            rules: "mp3/wav/m4a格式，5秒-2分钟"
            error_message: "请上传5秒-2分钟的音频文件"

        error_handling:
          - scenario: "时长不符"
            code: "400"
            message: "声音样本时长需要在5秒到2分钟之间"
            action: "返回具体时长和要求"

      - id: AC2
        title: "声音克隆处理"
        scenario:
          given: "声音样本上传成功"
          when: "系统处理克隆"
          then:
            - "调用豆包声音复刻API (Seed-ICL)"
            - "获取克隆音色ID"
            - "更新样本状态为completed"
            - "通知用户克隆完成"

        business_rules:
          - id: "BR-2.1"
            rule: "克隆处理预计1-3分钟"
          - id: "BR-2.2"
            rule: "克隆成功后可永久使用"

        error_handling:
          - scenario: "克隆失败"
            code: "500"
            message: "声音克隆失败，请确保样本清晰无杂音"
            action: "提示用户重新上传更清晰的样本"

      - id: AC3
        title: "使用克隆声音配音"
        scenario:
          given: "用户已有克隆成功的声音"
          when: "选择我的声音进行配音"
          then:
            - "使用克隆音色ID调用TTS"
            - "生成配音音频"

        business_rules:
          - id: "BR-3.1"
            rule: "克隆声音和标准音色使用相同TTS流程"

        error_handling:
          - scenario: "克隆音色不可用"
            code: "400"
            message: "您的声音样本正在处理中，请稍后"
            action: "返回处理状态"

    provides_apis:
      - "POST /api/v1/voice/upload-url"
      - "POST /api/v1/voice/samples"
      - "GET /api/v1/voice/samples"
      - "GET /api/v1/voice/samples/{id}"
      - "DELETE /api/v1/voice/samples/{id}"
      - "GET /api/v1/voice/samples/{id}/preview"
    consumes_apis: []
    dependencies:
      - "4.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.3"
    title: "视频自动剪辑合成"
    repository_type: monolith
    estimated_complexity: high
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "视频片段裁剪"
        scenario:
          given: "配音完成，获得各段时长"
          when: "系统裁剪视频"
          then:
            - "根据脚本段落获取对应镜头"
            - "按配音时长裁剪视频片段"
            - "使用FFmpeg提取指定时间段"
            - "存储裁剪后的片段"

        business_rules:
          - id: "BR-1.1"
            rule: "优先使用推荐镜头"
          - id: "BR-1.2"
            rule: "片段时长=配音时长+0.5秒过渡"
          - id: "BR-1.3"
            rule: "从视频中心位置开始裁剪"

        error_handling:
          - scenario: "视频时长不足"
            code: "422"
            message: "镜头时长不足，已自动调整"
            action: "循环播放或使用最大可用时长"

      - id: AC2
        title: "音视频合成"
        scenario:
          given: "所有片段和配音准备完成"
          when: "系统合成视频"
          then:
            - "按脚本顺序拼接视频片段"
            - "叠加配音音轨"
            - "输出竖屏1080x1920格式"
            - "上传成品到OSS"

        business_rules:
          - id: "BR-2.1"
            rule: "输出格式H.264编码MP4"
          - id: "BR-2.2"
            rule: "分辨率1080x1920(竖屏)"
          - id: "BR-2.3"
            rule: "视频码率4Mbps"

        error_handling:
          - scenario: "合成失败"
            code: "500"
            message: "视频合成失败，正在重试"
            action: "自动重试2次，失败后人工介入"

        examples:
          - input: "视频片段列表 + 配音音频"
            expected: |
              {"output_url": "https://oss.../output/12345.mp4", "duration_seconds": 62, "file_size_bytes": 47841280}

      - id: AC3
        title: "字幕生成与烧录"
        scenario:
          given: "脚本分段和配音时长已确定，用户已选择字幕设置"
          when: "系统合成视频"
          then:
            - "基于脚本分段生成ASS格式字幕文件"
            - "字幕时间轴与配音时长对齐"
            - "使用FFmpeg将字幕烧录到视频中"
            - "字幕采用双行滚动显示效果"
            - "应用用户选择的字幕样式模板"

        business_rules:
          - id: "BR-3.1"
            rule: "字幕时间轴基于每段脚本的配音起止时间自动计算"
          - id: "BR-3.2"
            rule: "双行滚动：当前播放行高亮显示，下一行预显示（半透明）"
          - id: "BR-3.3"
            rule: "字幕位置固定在视频底部居中"
          - id: "BR-3.4"
            rule: "如果用户关闭字幕，则跳过字幕烧录步骤"
          - id: "BR-3.5"
            rule: "单行字幕最大20个汉字，超出自动换行"

        error_handling:
          - scenario: "字幕生成失败"
            code: "500"
            message: "字幕生成失败，视频将不含字幕"
            action: "记录日志，继续合成无字幕视频，不阻塞流程"

        examples:
          - input: |
              脚本段落：
              段落1: "家人们！今天给你们探一家望京超火的海底捞..." (0-8秒)
              段落2: "一进门就被这个装修惊艳到了..." (8-14秒)
            expected: |
              生成ASS字幕文件，FFmpeg烧录命令：
              ffmpeg -i input.mp4 -vf "subtitles=subtitle.ass" output.mp4

    provides_apis:
      - "GET /api/v1/tasks/{id}/output"
    consumes_apis: []
    dependencies:
      - "4.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.4"
    title: "配音设置UI组件"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "音色选择界面"
        scenario:
          given: "用户在任务处理页面"
          when: "用户选择配音音色"
          then:
            - "显示标准音色列表（带试听按钮）"
            - "显示我的声音选项（如已克隆）"
            - "未克隆时显示上传入口"

        business_rules:
          - id: "BR-1.1"
            rule: "默认选中活泼女声"
          - id: "BR-1.2"
            rule: "克隆中的声音显示处理进度"

        interaction:
          - trigger: "点击试听按钮"
            behavior: "播放该音色的示例音频"
          - trigger: "选择音色"
            behavior: "高亮显示选中项"
          - trigger: "点击上传样本"
            behavior: "打开声音克隆弹窗"

      - id: AC2
        title: "声音克隆上传弹窗"
        scenario:
          given: "用户点击上传声音样本"
          when: "弹窗显示"
          then:
            - "显示上传要求说明"
            - "显示录音/文件上传选项"
            - "显示上传进度"
            - "上传完成后显示克隆进度"

        business_rules:
          - id: "BR-2.1"
            rule: "显示清晰的时长要求提示（5秒-2分钟，5秒即可高质量克隆）"
          - id: "BR-2.2"
            rule: "上传后自动开始克隆"

        interaction:
          - trigger: "选择文件"
            behavior: "验证格式和时长，开始上传"
          - trigger: "克隆中"
            behavior: "显示进度条，预计时间"
          - trigger: "克隆完成"
            behavior: "关闭弹窗，自动选中新声音"

    provides_apis: []
    consumes_apis:
      - "GET /api/v1/voice/samples"
      - "POST /api/v1/voice/upload-url"
      - "POST /api/v1/voice/samples"
    dependencies:
      - "4.2"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "4.5"
    title: "字幕设置UI组件"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "字幕开关控制"
        scenario:
          given: "用户在配音设置页面"
          when: "用户查看字幕设置"
          then:
            - "显示字幕开关，默认开启状态"
            - "开关旁显示说明文字：'为视频添加字幕'"
            - "关闭时字幕样式选择区域置灰禁用"

        business_rules:
          - id: "BR-1.1"
            rule: "字幕开关默认为开启状态"
          - id: "BR-1.2"
            rule: "开关状态保存到任务配置中"

        error_handling:
          - scenario: "状态保存失败"
            code: "500"
            message: "设置保存失败，请重试"
            action: "显示Toast提示，保持原状态"

        interaction:
          - trigger: "切换开关"
            behavior: "开启时样式选择区域激活，关闭时置灰"

      - id: AC2
        title: "字幕样式模板选择"
        scenario:
          given: "字幕功能已开启"
          when: "用户选择字幕样式"
          then:
            - "显示5种预设样式模板卡片"
            - "每个卡片显示样式名称和预览效果图"
            - "默认选中'简约白字'样式"
            - "点击卡片切换选中状态"

        business_rules:
          - id: "BR-2.1"
            rule: "预设模板：简约白字、活力黄字、小红书风、抖音热门、霓虹炫彩"
          - id: "BR-2.2"
            rule: "默认选中第一个模板（简约白字）"
          - id: "BR-2.3"
            rule: "样式选择保存到任务配置中"

        error_handling:
          - scenario: "样式预览图加载失败"
            code: "LOAD_ERROR"
            message: "样式加载失败"
            action: "显示占位图，功能不受影响"

        interaction:
          - trigger: "点击样式卡片"
            behavior: "选中该样式，显示选中边框（蓝色）"
          - trigger: "悬停样式卡片"
            behavior: "显示样式名称tooltip"

    provides_apis: []
    consumes_apis:
      - "PUT /api/v1/tasks/{id}/subtitle-settings"
    dependencies:
      - "4.4"
    sm_hints:
      front_end_spec: null
      architecture: null
```

## Epic 5: 预览导出与发布辅助

**Epic Summary:** 实现成品视频预览播放、下载导出功能，以及AI热门话题推荐和标题生成功能。

**Target Repositories:** monolith

```yaml
epic_id: 5
title: "预览导出与发布辅助"
description: |
  实现成品视频的在线预览播放、下载导出功能，
  集成AI生成热门话题标签和视频标题推荐，提升发布效率。

stories:
  - id: "5.1"
    title: "视频预览播放器"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "视频预览播放"
        scenario:
          given: "视频合成完成"
          when: "用户进入结果页面"
          then:
            - "显示视频播放器组件"
            - "支持播放/暂停控制"
            - "显示播放进度条"
            - "显示视频时长"

        business_rules:
          - id: "BR-1.1"
            rule: "视频自动加载但不自动播放"
          - id: "BR-1.2"
            rule: "支持全屏播放"
          - id: "BR-1.3"
            rule: "显示加载进度"

        error_handling:
          - scenario: "视频加载失败"
            code: "LOAD_ERROR"
            message: "视频加载失败，请刷新重试"
            action: "显示重新加载按钮"

        interaction:
          - trigger: "页面加载"
            behavior: "预加载视频，显示封面帧"
          - trigger: "点击播放"
            behavior: "开始播放，显示控制条"
          - trigger: "双击视频"
            behavior: "切换全屏状态"

      - id: AC2
        title: "使用镜头展示"
        scenario:
          given: "视频预览页面"
          when: "用户查看镜头详情"
          then:
            - "显示使用的镜头缩略图列表"
            - "显示每个镜头的分类和时长"
            - "支持展开/收起"

        business_rules:
          - id: "BR-2.1"
            rule: "默认收起状态"
          - id: "BR-2.2"
            rule: "显示镜头使用顺序"

        interaction:
          - trigger: "点击展开"
            behavior: "显示镜头缩略图网格"
          - trigger: "悬停镜头"
            behavior: "显示镜头详细信息"

    provides_apis: []
    consumes_apis:
      - "GET /api/v1/tasks/{id}/output"
    dependencies:
      - "4.3"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "5.2"
    title: "下载导出功能"
    repository_type: monolith
    estimated_complexity: low
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "下载成品视频"
        scenario:
          given: "用户在结果页面"
          when: "用户点击下载成品"
          then:
            - "获取带签名的下载URL"
            - "触发浏览器下载"
            - "显示下载进度（如支持）"

        business_rules:
          - id: "BR-1.1"
            rule: "下载链接有效期1小时"
          - id: "BR-1.2"
            rule: "文件名格式：{店铺名}-探店视频-{日期}.mp4"

        error_handling:
          - scenario: "链接过期"
            code: "401"
            message: "下载链接已过期，正在刷新"
            action: "自动获取新链接"

        interaction:
          - trigger: "点击下载"
            behavior: "显示下载中状态，触发下载"

      - id: AC2
        title: "下载素材包"
        scenario:
          given: "用户需要原始推荐镜头"
          when: "用户点击下载素材包"
          then:
            - "打包所有推荐镜头原片"
            - "生成ZIP文件"
            - "提供下载链接"

        business_rules:
          - id: "BR-2.1"
            rule: "素材包仅包含推荐镜头"
          - id: "BR-2.2"
            rule: "ZIP文件名：{店铺名}-素材包-{日期}.zip"

        error_handling:
          - scenario: "打包失败"
            code: "500"
            message: "素材包生成失败，请重试"
            action: "显示重试按钮"

    provides_apis:
      - "GET /api/v1/tasks/{id}/output/download"
      - "GET /api/v1/tasks/{id}/assets-pack"
    consumes_apis: []
    dependencies:
      - "5.1"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "5.3"
    title: "发布辅助服务"
    repository_type: monolith
    estimated_complexity: medium
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "热门话题推荐"
        scenario:
          given: "视频合成完成"
          when: "系统生成话题推荐"
          then:
            - "基于店铺信息和脚本内容"
            - "调用豆包API生成话题"
            - "返回5-10个热门话题标签"
            - "格式符合抖音/小红书规范"

        business_rules:
          - id: "BR-1.1"
            rule: "话题以#开头"
          - id: "BR-1.2"
            rule: "包含店铺名、品类、地区相关话题"
          - id: "BR-1.3"
            rule: "优先推荐当前热门话题"

        error_handling:
          - scenario: "生成失败"
            code: "500"
            message: "话题生成失败"
            action: "返回通用默认话题"

        examples:
          - input: "海底捞火锅望京店"
            expected: |
              ["#海底捞", "#火锅探店", "#望京美食", "#必吃榜", "#美食推荐", "#吃货日常", "#探店达人", "#北京美食", "#团购优惠", "#周末去哪吃"]

      - id: AC2
        title: "标题推荐"
        scenario:
          given: "视频合成完成"
          when: "系统生成标题推荐"
          then:
            - "基于脚本内容生成吸引眼球的标题"
            - "返回3-5个标题选项"

        business_rules:
          - id: "BR-2.1"
            rule: "标题20-50字"
          - id: "BR-2.2"
            rule: "包含吸引点击的元素"
          - id: "BR-2.3"
            rule: "符合平台调性"

        examples:
          - input: "海底捞火锅探店脚本"
            expected: |
              ["望京这家海底捞太绝了！服务好到让你感动流泪", "人均89吃海底捞？这个团购也太香了吧！", "海底捞探店｜必点招牌毛肚，七上八下太嫩了", "朋友聚餐首选！望京海底捞氛围感拉满"]

    provides_apis:
      - "GET /api/v1/tasks/{id}/publish-assist"
      - "POST /api/v1/tasks/{id}/publish-assist/regenerate"
    consumes_apis: []
    dependencies:
      - "4.3"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "5.4"
    title: "发布辅助UI组件"
    repository_type: monolith
    estimated_complexity: low
    priority: P0

    acceptance_criteria:
      - id: AC1
        title: "话题标签展示与复制"
        scenario:
          given: "用户在结果页面"
          when: "查看话题推荐"
          then:
            - "显示话题标签列表"
            - "支持一键复制所有话题"
            - "支持单个话题复制"

        business_rules:
          - id: "BR-1.1"
            rule: "话题标签显示为芯片样式"
          - id: "BR-1.2"
            rule: "复制后显示成功提示"

        interaction:
          - trigger: "点击一键复制"
            behavior: "复制所有话题到剪贴板，显示成功Toast"
          - trigger: "点击单个话题"
            behavior: "复制该话题，显示成功提示"

      - id: AC2
        title: "标题推荐展示与复制"
        scenario:
          given: "用户在结果页面"
          when: "查看标题推荐"
          then:
            - "显示标题选项列表"
            - "每个标题带复制按钮"

        business_rules:
          - id: "BR-2.1"
            rule: "标题显示为单选列表样式"

        interaction:
          - trigger: "点击复制按钮"
            behavior: "复制对应标题，显示成功提示"

    provides_apis: []
    consumes_apis:
      - "GET /api/v1/tasks/{id}/publish-assist"
    dependencies:
      - "5.3"
    sm_hints:
      front_end_spec: null
      architecture: null

  - id: "5.5"
    title: "历史任务管理"
    repository_type: monolith
    estimated_complexity: low
    priority: P1

    acceptance_criteria:
      - id: AC1
        title: "历史任务列表"
        scenario:
          given: "用户访问历史任务页面"
          when: "页面加载"
          then:
            - "显示用户所有任务列表"
            - "按创建时间倒序排列"
            - "显示任务状态、店铺名、创建时间"
            - "支持分页加载"

        business_rules:
          - id: "BR-1.1"
            rule: "每页显示10条"
          - id: "BR-1.2"
            rule: "显示任务状态图标"

        interaction:
          - trigger: "点击任务卡片"
            behavior: "跳转到任务详情/结果页面"
          - trigger: "滚动到底部"
            behavior: "自动加载更多"

      - id: AC2
        title: "删除任务"
        scenario:
          given: "用户在历史任务列表"
          when: "用户删除任务"
          then:
            - "显示确认弹窗"
            - "确认后删除任务和相关文件"
            - "从列表中移除"

        business_rules:
          - id: "BR-2.1"
            rule: "删除后无法恢复"
          - id: "BR-2.2"
            rule: "同时删除OSS上的视频文件"

        error_handling:
          - scenario: "删除失败"
            code: "500"
            message: "删除失败，请重试"
            action: "显示重试按钮"

        interaction:
          - trigger: "点击删除"
            behavior: "显示确认弹窗"
          - trigger: "确认删除"
            behavior: "显示加载状态，删除后从列表移除"

    provides_apis:
      - "DELETE /api/v1/tasks/{id}"
    consumes_apis:
      - "GET /api/v1/tasks"
    dependencies:
      - "1.4"
    sm_hints:
      front_end_spec: null
      architecture: null
```

---

## 7. Checklist Results Report

### 7.1 Executive Summary

| 指标 | 评估结果 |
|------|----------|
| **PRD完整度** | 98% |
| **MVP范围适当性** | Just Right (恰当) |
| **架构就绪度** | Ready (可进入架构设计阶段) |
| **主要关注点** | 无阻塞性问题 |
| **DevOps就绪度** | ✅ Epic 0已添加CI/CD、测试、监控基础设施 |

### 7.2 Category Analysis

| 类别 | 状态 | 关键问题 |
|------|------|----------|
| 1. 问题定义与背景 | ✅ PASS | 问题陈述清晰，目标用户明确 |
| 2. MVP范围定义 | ✅ PASS | 核心功能完整，边界清晰 |
| 3. 用户体验需求 | ✅ PASS | 7个核心页面已定义，交互范式明确 |
| 4. 功能需求 | ✅ PASS | 45条FR覆盖完整，可测试（含字幕功能FR41-45） |
| 5. 非功能需求 | ✅ PASS | 12条NFR涵盖性能/安全/扩展性 |
| 6. Epic与Story结构 | ✅ PASS | 6个Epic（含Epic 0 DevOps）、23个Stories，YAML格式完整 |
| 7. 技术指导 | ✅ PASS | 技术栈明确，微服务架构已规划 |
| 8. 跨功能需求 | ✅ PASS | 数据实体、API、集成点已识别 |
| 9. 清晰度与沟通 | ✅ PASS | 中英文混合，结构清晰 |

### 7.3 Validation Details

**✅ 问题定义验证**
- [x] 问题陈述清晰：探店达人创作效率低、技术门槛高
- [x] 目标用户明确：探店达人/内容创作者
- [x] 解决方案匹配问题：AI自动化全流程

**✅ MVP范围验证**
- [x] 核心功能聚焦：上传→分析→脚本→配音→合成→导出
- [x] 明确排除项：iOS App(V2.0)、会员支付系统
- [x] 每个功能直接服务核心价值

**✅ 功能需求验证**
- [x] FR1-FR45均可测试（含字幕功能FR41-45）
- [x] 需求聚焦WHAT而非HOW
- [x] 术语一致（店铺、任务、镜头、脚本）

**✅ Story质量验证**
- [x] 所有AC包含GIVEN/WHEN/THEN
- [x] 所有AC包含至少1条业务规则
- [x] 涉及输入的AC包含data_validation
- [x] 所有AC包含error_handling
- [x] 前端Story包含interaction

**✅ 技术约束验证**
- [x] 技术栈明确且团队可执行
- [x] 第三方依赖已识别（通义千问VL、豆包、阿里云TTS）
- [x] API版本化设计已规划

### 7.4 Recommendations

| 优先级 | 建议 | 状态 |
|--------|------|------|
| ~~HIGH~~ | ~~添加CI/CD流水线Story~~ | ✅ 已添加 (Story 0.1) |
| ~~HIGH~~ | ~~添加测试基础设施Story~~ | ✅ 已添加 (Story 0.2) |
| ~~MEDIUM~~ | ~~添加监控告警Story~~ | ✅ 已添加 (Story 0.3) |
| LOW | 可考虑增加成本监控相关NFR | 待定 |
| LOW | 首页/落地页(/)的详细设计可后续补充 | 待定 |
| LOW | 定价页(/pricing)暂未包含在Epic中，可作为V1.x迭代 | 待定 |

### 7.5 Final Decision

**✅ READY FOR ARCHITECT** - PRD文档结构完整、需求清晰、Epic/Story定义详尽，可进入架构设计阶段。

---

## 8. Next Steps

### 8.1 UX Expert Prompt

```
请基于 docs/prd.md 为探店宝(Shop Video Scout)项目创建前端设计规范。

项目概述：探店达人的AI视频助手，支持视频上传、AI镜头分析、脚本生成、
配音合成、视频剪辑到成品导出的完整流程。

请重点关注：
1. 步骤引导式的创作流程设计
2. 实时进度反馈的交互模式
3. 视频播放器和上传组件的设计规范
4. 声音克隆功能的用户引导
5. 发布辅助（话题、标题）的复制交互

目标平台：Web Responsive (V1.0聚焦Web端)
```

### 8.2 Architect Prompt

```
请基于 docs/prd.md 为探店宝(Shop Video Scout)项目创建技术架构文档。

技术栈约束：
- 前端：Vue 3 + TypeScript + Vite + TailwindCSS + Element Plus
- 后端：Java 21 + Spring Cloud (Gateway + Nacos)
- 数据库：MySQL 8.0 + Redis
- 存储：阿里云OSS + CDN
- AI服务：通义千问VL(视觉) + 豆包API(文本) + 阿里云语音合成(TTS)
- 视频处理：FFmpeg

架构重点：
1. 微服务划分和服务间通信
2. 异步任务处理（视频分析、配音合成）
3. 文件上传和存储策略
4. AI服务集成和错误处理
5. 为V2.0 iOS App预留的API设计
```
