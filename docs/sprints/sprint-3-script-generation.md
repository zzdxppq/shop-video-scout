# Sprint 3: AI脚本生成

## Sprint 信息

| 属性 | 值 |
|------|-----|
| Sprint 编号 | 3 |
| Sprint 名称 | AI Script Generation |
| 状态 | Planned |
| 创建日期 | 2026-01-22 |

## Sprint 目标

完成 AI 脚本生成和编辑功能，建立测试基础设施保障代码质量。

## 交付物

- AI 脚本生成服务 (豆包 API)
- 脚本编辑器页面
- 测试基础设施 (单元测试 + 集成测试)

## Sprint Backlog

### Story 3.1: AI脚本生成服务
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Backend |
| 依赖 | 2.3 |
| 文件 | [3.1.ai-script-generation.md](../stories/3.1.ai-script-generation.md) |

**Tasks:**
- [ ] T1: 豆包 API 集成
- [ ] T2: 脚本生成 Prompt 设计
- [ ] T3: 脚本段落与镜头关联

---

### Story 3.2: 脚本编辑页面
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | Frontend |
| 依赖 | 3.1 |
| 文件 | [3.2.script-editor-ui.md](../stories/3.2.script-editor-ui.md) |

**Tasks:**
- [ ] T1: 脚本段落编辑组件
- [ ] T2: 镜头-脚本关联展示
- [ ] T3: 重新生成功能

---

### Story 0.2: 测试基础设施搭建
| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 类型 | DevOps |
| 依赖 | 1.1 |
| 文件 | [0.2.test-infrastructure.md](../stories/0.2.test-infrastructure.md) |

**Tasks:**
- [ ] T1: JUnit 5 + Mockito 配置
- [ ] T2: Vitest + Vue Test Utils 配置
- [ ] T3: 测试覆盖率报告集成

---

## 依赖关系

```
[2.3 AI分析] ──► [3.1 AI脚本生成] ──► [3.2 脚本编辑UI]

[1.1 后端微服务] ──► [0.2 测试基础设施] (并行)
```

## 验收标准

- [ ] AI 可根据镜头分析结果生成探店脚本
- [ ] 脚本按段落展示，关联对应镜头
- [ ] 用户可编辑脚本内容
- [ ] 用户可触发重新生成
- [ ] 后端单元测试覆盖率 > 60%
- [ ] 前端组件测试覆盖率 > 50%

## 风险与阻塞

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| AI 生成质量不稳定 | 用户体验差 | 优化 Prompt，增加示例 |
| Prompt 迭代频繁 | 开发效率低 | 使用配置化 Prompt 管理 |

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-22 | SM Agent | Sprint 创建 |
