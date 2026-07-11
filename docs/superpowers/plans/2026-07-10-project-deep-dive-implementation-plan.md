# 项目经历深挖模块实施计划

依据：`docs/superpowers/specs/2026-07-10-project-deep-dive-design.md`

## 总体原则

- 分三个可运行、可测试、可回滚的实施阶段推进。
- 现有 `JAVA_CORE` 八股接口保持兼容。
- 外部 LLM 调用不占用数据库长事务。
- Milvus 第一批使用 NoOp 适配器，不阻塞项目面试。
- 学习指南仅保留本地，不加入项目提交。

## 阶段一：基础模型与项目档案

### 数据库与配置

- 添加 Flyway MySQL 依赖。
- 建立当前数据库基线迁移。
- 建立项目深挖核心表和 `interview_session` 兼容迁移。
- 将核心手工题卡和深挖模板改为幂等 Repeatable Migration。
- 禁用 Spring SQL 自动初始化。
- 将大型生成题库改为显式导入，不随应用启动执行。

### 安全与错误

- 增加领域错误码和统一业务异常。
- 增加资源访问令牌生成、HMAC 摘要和 Header 校验。
- 更新环境变量模板和生产 Compose。

### 项目档案

- 创建 ProjectProfile、ProjectClaim、ProjectProbeTemplate 实体和 Mapper。
- 实现创建、读取、分析、修正和确认接口。
- 实现项目文本脱敏。
- 实现 LLM 结构化提取及输出校验。
- 实现 DRAFT → ANALYZING → REVIEW_REQUIRED → READY 状态流。

### 阶段一验证

- 新数据库迁移测试。
- 已有本地数据库升级测试。
- 项目档案状态机和访问令牌测试。
- LLM 合法 JSON、坏 JSON和失败状态测试。
- 现有八股后端测试和前端构建回归。

## 阶段二：项目面试闭环

- 创建 InterviewPlan、InterviewTurn、TurnEvaluation 持久化实现。
- 实现 ProjectInterviewPlanner。
- 实现 ProjectDeepDivePolicy 和 InterviewStateMachine。
- 实现同步 `/turns` 回答处理流水线。
- 实现 `clientTurnId` 幂等、version 并发控制和过期 PROCESSING 恢复。
- 实现 NoOpVectorRetrievalService 和统一 InterviewContextAssembler。
- 实现多轮追问、切换维度、切换 Claim 和自然收尾。
- 保持旧 `/answers`、`/next-question`、`/report` 契约不变。

## 阶段三：报告与前端体验

- 实现结构化 ReportAggregator 和可选 ReportNarrativeGenerator。
- 扩展报告持久化和逐轮证据引用。
- 将前端迁移到 Vue Router 页面结构。
- 建设项目输入与提取确认页。
- 建设沉浸式文字面试室。
- 建设项目报告页。
- 使用 sessionStorage 保存资源令牌和项目草稿。
- 增加 Vitest、Vue Test Utils 和关键 Composable/组件测试。

## 最终验收与交付

- 执行设计规格第 20、21 节逐项完成审计。
- 后端单元测试、集成测试通过。
- 前端测试、类型检查、生产构建通过。
- 生产 Compose 校验通过。
- 手工完成至少一个黄金项目的端到端面试。
- 检查密钥、敏感信息和未跟踪文件。
- 仅提交项目功能和正式设计文档，不提交本地学习指南。
- 推送学校 GitLab `origin/main`。
- 推送 GitHub 自动部署分支。
- 检查 CI/CD 和生产健康状态。
