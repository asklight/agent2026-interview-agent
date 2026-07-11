# 项目经历深挖模块设计规格

日期：2026-07-10
状态：已确认并完成首批实现
适用仓库：`agent2026-interview-agent`

## 1. 背景

当前项目已经具备 Java 后端八股练习的最小闭环：结构化题卡、用户回答、LLM 评价、一次追问和训练报告。

现有实现更接近“AI 评分题库”，尚不足以支撑项目最初承诺的真实 Java 技术一面。下一阶段将项目经历深挖建设为独立业务模块，并为后续综合模拟面试和沉浸式语音面试提供基础。

项目深挖不是把简历中的技术关键词转换成固定八股题，而是围绕用户声称做过的项目，验证：

- 项目和经历是否真实；
- 用户本人承担了哪些职责；
- 指标和成果是否可信；
- 是否理解所使用技术的原理；
- 是否理解方案取舍；
- 是否经历并处理过故障和边界情况；
- 系统规模变化后是否能够判断瓶颈。

## 2. 模块定位

项目级业务模块划分：

```text
Java 面试训练平台
├── 八股练习模块
├── 项目深挖模块             # 本规格范围
├── 综合模拟面试模块
├── 算法口述模块
└── 训练历史与复盘模块
```

八股练习与项目深挖是独立业务模块：

| 对比项 | 八股练习 | 项目深挖 |
| --- | --- | --- |
| 用户目标 | 快速检查知识掌握程度 | 训练项目表达、真实性和工程能力 |
| 问题起点 | 结构化知识题卡 | 用户项目档案和项目声明 |
| 追问次数 | 每题最多一次 | 每个核心声明连续两到三次 |
| 反馈时机 | 逐题即时反馈 | 面试结束后统一反馈 |
| 页面体验 | 轻量练习页 | 沉浸式面试页 |
| 后续语音 | 可选 | 优先接入 |

两者共享会话、轮次、评价、知识检索、LLM 调用和报告聚合等基础能力，但不共享具体业务策略。

## 3. 目标

第一批交付完成后，用户可以：

1. 粘贴一段项目经历描述；
2. 由系统提取项目名称、技术栈、个人职责、关键指标和需要验证的项目声明；
3. 对明显错误的提取结果进行确认或修正；
4. 发起一场 10 到 20 分钟的文字项目面试；
5. 接受围绕项目真实性、技术原理、指标、取舍和故障边界的多轮追问；
6. 面试过程中只看到自然的面试官消息，不看到分数、命中点和内部动作；
7. 面试结束后查看基于真实对话证据生成的逐轮复盘。

## 4. 非目标

本批次不实现：

- 简历 PDF、Word 文件上传和 OCR；
- 完整简历管理系统；
- 实时语音识别、TTS 和 WebSocket 音频流；
- 保存原始音频；
- Milvus 数据清洗、灌库和生产部署；
- 综合模拟面试中的八股、项目和算法自动编排；
- 完整登录、组织架构和 RBAC；
- 项目档案后台管理系统；
- 多人协作和面试官人工介入。

本批次必须为 Milvus 和语音保留稳定接口，但不能让这些后续能力阻塞文字项目面试闭环。

## 5. 总体设计原则

### 5.1 模块化单体

继续使用 Spring Boot 模块化单体，不新增线上 Python Agent 服务。

原因：

- 当前团队和系统规模不足以支撑多服务带来的部署、监控和联调成本；
- tju-llm 由 Spring Boot 统一代理即可；
- Milvus 灌库工具可以保留为后续离线 `apps/ingest`；
- 未来复杂度确实增长后，再通过稳定端口拆分服务。

### 5.2 垂直业务模块

项目深挖代码按业务模块集中，而不是将文件全部散落到全局 `controller/service/mapper/vo` 目录。

推荐目标结构：

```text
com.agent2026.interview
├── shared
│   ├── api
│   ├── error
│   └── config
├── interviewcore
│   ├── application
│   ├── domain
│   ├── port
│   └── infrastructure
├── knowledge
│   ├── structured
│   ├── retrieval
│   ├── context
│   └── integration
├── knowledgepractice
└── projectdeepdive
    ├── api
    ├── application
    ├── domain
    └── infrastructure
```

第一批不要求一次性搬迁全部旧代码。采用渐进式迁移：新模块按照目标结构建设，现有八股接口保持兼容，再逐步把共享逻辑抽入 `interviewcore`。

### 5.3 单向依赖

```text
API → Application → Domain
                  ↑
          Infrastructure 实现 Domain 定义的端口
```

约束：

- Domain 不依赖 Spring MVC、MyBatis、HTTP、Milvus 或具体 LLM 客户端；
- API 层不拼 Prompt、不操作 Mapper、不实现状态机；
- Application 层负责编排用例和事务边界；
- Infrastructure 只实现存储和外部服务适配；
- 跨模块调用通过公开接口完成，不读取其他模块内部 Mapper。

### 5.4 外部调用不占用长事务

LLM 和未来 Milvus 调用必须位于数据库短事务之外。

典型回答处理过程：

```text
短事务：校验状态并登记 PROCESSING Turn
→ 事务外调用检索和 LLM
→ 短事务：保存评价、面试官消息和新状态
```

## 6. 三层知识架构

原项目决策保持不变：

```text
第一层：结构化题卡或结构化深挖模板
第二层：Milvus 向量知识召回
第三层：tju-llm 动态评价、追问和复盘
```

信息优先级：

```text
结构化评分规则 > Milvus 召回内容 > LLM 自身知识
```

### 6.1 第一层：结构化深挖模板

项目深挖不直接使用固定八股题序，而是使用结构化深挖模板定义必须验证的维度。

首批维度：

| 维度 | 目标 |
| --- | --- |
| `AUTHENTICITY` | 验证项目是否真实做过 |
| `OWNERSHIP` | 验证个人职责和协作边界 |
| `METRIC` | 验证指标、基线和测量方法 |
| `PRINCIPLE` | 验证涉及技术的底层理解 |
| `TRADEOFF` | 验证选型和替代方案取舍 |
| `INCIDENT` | 验证故障、降级和排查能力 |
| `SCALE` | 验证扩容后的瓶颈判断 |

模板负责稳定的目标、证据要求、评分规则和追问边界，不提供固定项目答案。

### 6.2 第二层：向量知识召回

定义统一端口：

```java
public interface VectorRetrievalService {
    RetrievalContext retrieve(RetrievalQuery query);
}
```

第一批提供：

- `NoOpVectorRetrievalService`：Milvus 未配置时返回空上下文；
- 稳定的请求、响应和追踪数据结构；
- 检索失败时的降级日志和指标。

后续提供：

- `MilvusVectorRetrievalService`；
- `apps/ingest` 清洗、切片、Embedding 和灌库流程；
- 八股总结、真实面经、项目追问案例和故障案例的语义检索。

### 6.3 第三层：tju-llm

LLM 负责：

- 提取项目档案和项目声明；
- 将项目声明、深挖模板、最近对话和召回知识组合为自然问题；
- 生成结构化评价；
- 提出针对当前回答缺口的连续追问；
- 生成自然转场和收尾；
- 生成报告文本。

LLM 不允许直接决定最终状态。所有决策必须由后端状态机校验和修正。

## 7. 核心领域模型

### 7.1 ProjectProfile

表示用户的一段项目经历。

核心字段：

```text
id
access_token_hash
sanitized_description
project_name
summary
tech_stack_json
responsibilities_json
metrics_json
architecture_json
uncertainties_json
analysis_status
version
create_time
update_time
```

`analysis_status`：

```text
DRAFT
ANALYZING
REVIEW_REQUIRED
READY
FAILED
```

### 7.2 ProjectClaim

表示项目描述中需要被面试验证的一条声明。

示例：

```text
“使用 Redis 缓存后，接口 P95 延迟降低了 60%。”
```

核心字段：

```text
id
project_profile_id
claim_type
statement
source_fragment
related_technologies_json
expected_evidence_json
risk_level
confirmed
create_time
```

`claim_type` 首批支持：

```text
RESPONSIBILITY
TECHNICAL_CHOICE
PERFORMANCE_IMPROVEMENT
ARCHITECTURE_DESIGN
INCIDENT_HANDLING
BUSINESS_RESULT
```

### 7.3 ProjectProbeTemplate

表示第一层知识中的结构化深挖规则。

核心字段：

```text
id
template_code
dimension
applicable_tags_json
objective
required_evidence_json
scoring_rubric_json
follow_up_rules_json
enabled
version
create_time
update_time
```

### 7.4 InterviewPlan

表示创建会话时生成的本次面试计划快照。

核心字段：

```text
id
session_id
project_profile_snapshot_json
planned_probes_json
template_version
status
create_time
```

计划必须保存快照，避免项目档案或模板修改后无法解释历史面试。

### 7.5 InterviewSession

现有会话模型扩展为统一模式会话。

新增或替换字段：

```text
mode                         JAVA_CORE / PROJECT_DEEP_DIVE
feedback_timing              IMMEDIATE / AFTER_SESSION
status                       复用现有列并扩展语义
conversation_phase
project_profile_id
current_claim_id
current_probe_dimension
follow_up_count
max_follow_up_count
input_modality               TEXT / VOICE_TRANSCRIPT
version
```

兼容策略：

- 第一批保留现有 `JAVA_CORE` 值，不直接重命名为 `KNOWLEDGE_PRACTICE`；
- 领域层将 `JAVA_CORE` 映射到知识练习策略；
- `interview_session.module` 和 `current_question_type` 对项目模式改为可空；
- 继续复用现有 `status` 列，不新增含义重复的 `lifecycle_status`；
- 旧八股 `/answers`、`/next-question` 和现有 VO 保持不变；
- 项目深挖使用新的 `/turns` 契约；
- 第一批不迁移或双写旧 `interview_answer`，通过 Legacy Adapter 保持旧八股链路，后续再单独迁移到统一 Turn 模型。

### 7.6 InterviewTurn

统一保存面试官、候选人和系统消息。

核心字段：

```text
id
session_id
sequence_no
role                         INTERVIEWER / CANDIDATE / SYSTEM
turn_type                    OPENING / MAIN / FOLLOW_UP / ANSWER / TRANSITION / CLOSING
content
input_modality               TEXT / VOICE_TRANSCRIPT
parent_turn_id
claim_id
probe_dimension
processing_status
processing_started_at
client_turn_id
started_at
ended_at
create_time
```

约束：

```text
UNIQUE(session_id, sequence_no)
UNIQUE(session_id, client_turn_id)
```

`client_turn_id` 防止重复点击和网络重试导致重复调用 LLM。

### 7.7 TurnEvaluation

评价与用户可见消息分离保存。

核心字段：

```text
id
session_id
candidate_turn_id
score_json
hit_points_json
missing_points_json
weaknesses_json
evidence_json
risk_flags_json
decision
suggested_follow_up
retrieval_trace_json
create_time
```

第一批不持久化完整 `model_raw_response`，实际字段替换为：

```text
model_response_hash
model_schema_version
degraded
```

经过校验的结构化评价是事实来源。调试日志也不得记录完整模型响应。

首版 `score_json` Schema 固定为：

```json
{
  "schemaVersion": 1,
  "authenticity": 72,
  "ownership": 80,
  "technicalDepth": 65,
  "tradeoffReasoning": 70,
  "engineeringAwareness": null,
  "communication": 75
}
```

规则：

- 每个维度范围为 0 到 100；
- 尚未覆盖的维度使用 `null`，报告中显示 `NOT_ASSESSED`；
- 未覆盖维度不参与总分计算；
- 风险标记只能表述为“证据不足”“前后不一致”或“需要进一步验证”，不得直接判定用户造假；
- 最终总分只聚合已覆盖维度，并在报告中同时展示覆盖度。

项目深挖进行中时，API 不返回内部评价字段。

## 8. 状态机

### 8.1 项目档案状态

```text
DRAFT
→ ANALYZING
→ REVIEW_REQUIRED
→ READY

ANALYZING
→ FAILED
→ ANALYZING（重试）
```

规则：

- `READY` 前不能创建项目面试；
- 同一档案同时只能有一个分析任务；
- 用户通过明确的确认动作将档案从 `REVIEW_REQUIRED` 推进到 `READY`；
- 修改 `READY` 档案的核心字段后，状态重新变为 `REVIEW_REQUIRED`；
- 修改确认后的核心字段会增加版本号；
- 面试计划保存项目档案快照。

### 8.2 会话生命周期

```text
PREPARING
→ IN_PROGRESS
→ FINISHING
→ FINISHED

PREPARING / IN_PROGRESS
→ CANCELLED
```

### 8.3 对话阶段

```text
OPENING
→ PROJECT_OVERVIEW
→ CLAIM_DEEP_DIVE
→ TECHNICAL_PROBE
→ TRADEOFF_OR_INCIDENT
→ WRAP_UP
```

### 8.4 单次回答处理状态

```text
AWAITING_ANSWER
→ EVALUATING
→ AWAITING_ANSWER

EVALUATING
→ RETRYABLE_ERROR
→ EVALUATING
```

### 8.5 下一步决策

```text
FOLLOW_UP
SWITCH_DIMENSION
SWITCH_CLAIM
WRAP_UP
FINISH
```

后端强制规则：

- 同一个 Claim 连续追问不超过 `max_follow_up_count`；
- 不能长期停留在单一八股知识点；
- 系统自动结束前应覆盖 `OWNERSHIP`、`AUTHENTICITY`、`PRINCIPLE` 和 `TRADEOFF`；
- 用户主动结束始终允许，未覆盖维度在报告中标记为 `NOT_ASSESSED`，不按零分处理；
- `follow_up_count` 表示当前 Claim 追问链中主问之后的追问次数，切换 Claim 时归零；
- 历史追问次数从 `InterviewTurn` 推导；
- `planned_probes_json` 中的每项必须有稳定 `probeId`，Turn 和 Evaluation 引用该 ID；
- `durationMinutes` 仅作为计划提示和 UI 预计时长，不作为第一批硬超时；自动收尾依据计划覆盖量和最大轮数；
- 达到覆盖阈值、最大轮数或用户主动结束时自然收尾；
- LLM 返回非法状态时，由状态机选择合法保底决策。

## 9. 核心组件

### 9.1 Application

```text
ProjectProfileApplicationService
DeepDiveInterviewApplicationService
InterviewReportApplicationService
InterviewOrchestrator
```

职责：

- `ProjectProfileApplicationService`：项目档案创建、分析、修改和确认；
- `DeepDiveInterviewApplicationService`：项目深挖公开用例入口；
- `InterviewOrchestrator`：统一执行一次回答处理流水线；
- `InterviewReportApplicationService`：结束会话、生成和读取报告；
- Application Service 调用领域策略和端口、控制短事务并返回应用 DTO；
- Application Service 不直接拼接页面 VO。

### 9.2 Domain

```text
InterviewModePolicy
KnowledgePracticePolicy
ProjectDeepDivePolicy
InterviewStateMachine
ProjectInterviewPlanner
TurnEvaluator
ReportAggregator
ReportNarrativeGenerator
```

职责固定为：

- `ProjectInterviewPlanner` 只生成计划；
- `TurnEvaluator` 只产生评价事实和 LLM 建议，不修改会话状态；
- `ProjectDeepDivePolicy` 根据评价、计划、证据覆盖和追问次数提出领域决策；
- `InterviewStateMachine` 校验并执行最终合法迁移；
- `ReportAggregator` 确定性聚合结构化事实；
- `ReportNarrativeGenerator` 可选地使用 LLM 润色报告文案，不得增加结构化事实中不存在的风险结论。

核心接口示例：

```java
public interface InterviewModePolicy {
    InterviewPlan createPlan(PlanningContext context);
    TurnDecision decideNext(TurnContext context);
    FeedbackTiming feedbackTiming();
}
```

### 9.3 Knowledge

```text
StructuredKnowledgeProvider
ProjectProbeTemplateProvider
VectorRetrievalService
InterviewContextAssembler
```

`InterviewContextAssembler` 负责固定上下文优先级，避免不同 Service 随意拼 Prompt。

### 9.4 Integration

```text
TjuLlmGateway
NoOpVectorRetrievalService
MilvusVectorRetrievalService（后续）
```

LLM 输出必须经过强类型结构校验。JSON 结构解析失败时最多发起一次结构修复调用；网络超时不自动连续重试，当前 Turn 标记为可重试。只有结构化模板存在安全保底追问时才允许降级继续，并设置 `degraded=true`；无法形成可靠评价时不生成分数。

## 10. 业务流程

### 10.1 创建并分析项目档案

```text
用户输入项目描述
→ 敏感信息检测和文本清洗
→ 短事务保存 DRAFT Profile
→ 更新为 ANALYZING
→ 事务外调用 tju-llm 提取结构化数据
→ 校验提取结果
→ 短事务保存 Profile 和 Claim
→ 状态变为 READY
→ 返回提取确认结果
```

前端交互原则：

- 默认只有一个主要文本框；
- 技术栈、职责、指标和 Claim 自动提取；
- 用户只修正明显错误；
- 难度、时长等放在折叠的高级设置；
- 不设计复杂简历编辑器。

### 10.2 创建项目面试

```text
READY ProjectProfile
→ 选择适用的 ProjectProbeTemplate
→ 根据 Claim 风险和价值排序
→ 调用 VectorRetrievalService 获取增强知识
→ ProjectInterviewPlanner 生成计划
→ 保存 InterviewPlan 快照
→ 创建 Session 和 Opening Turn
→ 返回面试室初始数据
```

推荐首问：

> 请先用一到两分钟介绍这个项目，并重点说明你本人负责的部分。

### 10.3 提交回答并生成下一轮问题

```text
提交 clientTurnId 和回答文本
→ 短事务校验 accessToken、状态和幂等键
→ 保存 PROCESSING Candidate Turn
→ 事务外构建上下文
   ├── 当前 ProjectClaim
   ├── 当前 ProbeTemplate
   ├── 最近对话
   ├── 已覆盖和未覆盖证据
   └── 向量召回上下文
→ 事务外调用 tju-llm
→ 校验结构化评价
→ 状态机修正下一步决策
→ 短事务保存 TurnEvaluation、Interviewer Turn 和会话新状态
→ 前端只返回面试官消息和公开进度
```

项目面试响应不包含：

- 分数；
- 命中点；
- 遗漏点；
- 弱点；
- 内部决策枚举；
- 模型原始响应；
- 检索内部信息。

### 10.4 结束并生成报告

```text
用户主动结束或计划完成
→ 会话进入 FINISHING
→ 聚合 TurnEvaluation
→ 按 Claim 和维度生成报告
→ 保存报告
→ 会话进入 FINISHED
```

报告内容：

- 综合表现和维度评分；
- 项目表达亮点；
- 真实性风险；
- 技术原理薄弱点；
- 方案取舍和故障意识；
- 逐轮对话复盘；
- 推荐回答结构；
- 下一轮训练建议；
- 后续 Milvus 推荐资料。

报告生成分为两步：

1. `ReportAggregator` 从结构化评价中确定性聚合事实、分数、覆盖度和证据引用；
2. `ReportNarrativeGenerator` 可选地使用 LLM 润色文字，但不得创建未被 Evaluation 支持的新结论。

每条关键报告结论必须至少引用一个：

```text
claimId
candidateTurnId
evaluationId
```

## 11. API 契约

### 11.1 项目档案

```text
POST  /api/project-profiles
GET   /api/project-profiles/{profileId}
PATCH /api/project-profiles/{profileId}
POST  /api/project-profiles/{profileId}/analyze
POST  /api/project-profiles/{profileId}/confirm
```

创建请求：

```json
{
  "description": "项目经历文本"
}
```

创建响应返回：

- `profileId`；
- `accessToken`，仅创建时返回明文；
- `analysisStatus`；
- 脱敏后的项目描述。

分析响应返回：

- 项目名称和摘要；
- 技术栈；
- 个人职责；
- 指标；
- Claim 列表；
- 不确定项和用户确认提示。

接口语义固定为：

- `POST /api/project-profiles`：只保存脱敏后的 `DRAFT`；
- `POST /api/project-profiles/{id}/analyze`：分析完成后进入 `REVIEW_REQUIRED`；
- `PATCH /api/project-profiles/{id}`：修正提取字段和 Claim；
- `POST /api/project-profiles/{id}/confirm`：确认后进入 `READY`。

### 11.2 创建统一面试会话

```text
POST /api/interview-sessions
```

项目深挖请求：

```json
{
  "mode": "PROJECT_DEEP_DIVE",
  "projectProfileId": 12,
  "durationMinutes": 20,
  "maxFollowUpsPerClaim": 3,
  "inputModality": "TEXT"
}
```

现有八股模式保持兼容：

```json
{
  "mode": "JAVA_CORE",
  "module": "Redis",
  "difficulty": "medium",
  "questionCount": 5,
  "inputModality": "TEXT"
}
```

### 11.3 对话轮次

```text
GET  /api/interview-sessions/{sessionId}/turns
POST /api/interview-sessions/{sessionId}/turns
POST /api/interview-sessions/{sessionId}/finish
```

提交请求：

```json
{
  "clientTurnId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "候选人的回答",
  "inputModality": "TEXT"
}
```

未来语音识别确认后使用相同接口：

```json
{
  "clientTurnId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "用户确认后的语音转写文本",
  "inputModality": "VOICE_TRANSCRIPT"
}
```

原始音频不进入领域接口、不保存到数据库。

第一批使用同步 `POST /turns`，不实现 SSE。前端提交后显示“面试官正在思考”，等待完整面试官消息返回。实时流式协议留到语音阶段单独设计。

Turn 处理状态固定为：

```text
PROCESSING
COMPLETED
RETRYABLE_FAILED
```

幂等与恢复规则：

- 相同 `clientTurnId` 对应 `COMPLETED`：直接返回第一次处理结果；
- 对应 `PROCESSING` 且租约未过期：返回 `409 INTERVIEW_TURN_PROCESSING`；
- 对应 `RETRYABLE_FAILED`：允许使用原 `clientTurnId` 重试；
- `PROCESSING` 超过 120 秒视为租约过期，可由下次请求转为 `RETRYABLE_FAILED`；
- 服务进程中断后不会永久卡死在 `PROCESSING`。

创建请求采用按 `mode` 条件校验：

- `JAVA_CORE` 必须提供 `module`；
- `PROJECT_DEEP_DIVE` 必须提供 `projectProfileId`；
- 不再在统一参数对象上对 `module` 使用无条件 `@NotBlank`。

### 11.4 资源归属

当前未实现正式登录系统。第一批为每个项目档案生成一个不可猜测的资源访问令牌，项目面试会话继承该项目档案的令牌，不再生成第二套会话令牌。

契约固定为：

```text
请求 Header：X-Resource-Token
数据库字段：project_profile.access_token_hash
响应字段：accessToken，仅创建项目档案时返回一次明文
前端存储：sessionStorage
```

- 数据库仅保存令牌的 HMAC-SHA-256 摘要，不保存明文；
- 所有 Profile、Session、Turn 和 Report 读写接口都校验 `X-Resource-Token`；
- 页面刷新后从 `sessionStorage` 恢复令牌，再从后端拉取档案或完整对话；
- 令牌丢失时第一批不提供找回能力，用户需要重新创建档案；
- 未来接入用户登录后，资源令牌由用户身份和资源归属校验替代；
- 业务接口不依赖连续自增 ID 作为授权依据。

## 12. 前端设计

### 12.1 路由

```text
/
/practice/knowledge
/project-deep-dive
/project-deep-dive/new
/project-deep-dive/{profileId}
/interview/{sessionId}
/interview/{sessionId}/report
```

`/interview/{sessionId}` 使用独立 `InterviewLayout`，不套用固定侧边栏。

### 12.2 前端模块结构

```text
src/features/project-deep-dive/
├── api/
│   ├── projectDeepDiveApi.ts
│   └── interviewApi.ts
├── model/
│   ├── types.ts
│   └── events.ts
├── stores/
│   └── interviewSession.ts
├── composables/
│   ├── useProjectDraft.ts
│   ├── useInterviewSession.ts
│   └── useVoiceInput.ts
├── components/
│   ├── ProjectDescriptionForm.vue
│   ├── ProjectExtractionReview.vue
│   ├── ProjectClaimCard.vue
│   ├── InterviewMessageList.vue
│   ├── InterviewMessageItem.vue
│   ├── InterviewComposer.vue
│   ├── VoiceControl.vue
│   ├── ReportDimensionCard.vue
│   └── TurnReview.vue
└── views/
    ├── ProjectDeepDiveHome.vue
    ├── ProjectSetup.vue
    ├── InterviewRoom.vue
    └── InterviewReport.vue
```

### 12.3 页面原则

项目设置页：

- 一个主文本框；
- 自动保存草稿；
- 分析后在同页展示提取结果；
- 一次确认进入面试；
- 高级参数默认折叠。

面试室：

- 连续对话时间线；
- 面试官、候选人和系统消息角色清晰；
- 显示计时、连接状态和结束按钮；
- 不显示题号导航、分数、命中点和系统动作；
- 输入组件支持 `TEXT` 和未来 `VOICE_TRANSCRIPT`；
- 页面刷新后从后端恢复完整对话。

报告页：

- 综合评价与维度评分；
- 逐 Claim 和逐轮复盘；
- 项目真实性、技术深度、职责、取舍和沟通表达分区；
- 每条结论可以追溯到具体回答。

### 12.4 前端状态管理

Pinia 只保存跨路由状态：

- `profileId`；
- `sessionId`；
- 访问令牌；
- 会话阶段；
- 连接状态；
- 输入模态。

完整对话和报告以后端为事实来源。输入框、弹窗和局部 Loading 使用组件或 Composable 本地状态。

第一批页面通过 Typed API 调用同步 `/turns` 接口。未来增加 SSE 或 WebSocket 时，协议解析必须位于独立客户端或 Composable 中，页面组件不能直接解析底层流式协议。

## 13. 错误处理

引入稳定的领域错误码，逐步替换通用 `IllegalStateException`。

```text
PROJECT_PROFILE_NOT_FOUND
PROJECT_PROFILE_ANALYSIS_FAILED
PROJECT_PROFILE_NOT_READY
PROJECT_PROFILE_ACCESS_DENIED
INTERVIEW_SESSION_NOT_FOUND
INTERVIEW_SESSION_FINISHED
INTERVIEW_SESSION_ACCESS_DENIED
INTERVIEW_STATE_CONFLICT
INTERVIEW_DUPLICATE_TURN
INTERVIEW_TURN_PROCESSING
LLM_UNAVAILABLE
LLM_RESPONSE_INVALID
REPORT_NOT_READY
```

处理策略：

- Milvus 超时：记录降级事件，返回空召回，面试继续；
- LLM 超时：当前 Turn 标记为可重试，不推进会话；
- LLM JSON 非法：最多一次结构修复调用；有安全模板追问时降级继续，否则标记为可重试；
- 重复提交：根据 `client_turn_id` 返回第一次处理结果；
- 并发提交：使用数据库 `version` 条件更新；
- 报告重复生成：使用 `session_id` 唯一键保证幂等；
- 敏感文本：日志不打印完整项目原文和完整 Prompt；
- 未知异常：对用户返回通用提示，对日志记录 `traceId` 和内部原因。

`KNOWLEDGE_RETRIEVAL_DEGRADED` 不是用户错误码。Milvus 降级只记录为内部诊断事件、Metrics 标签，并可在响应的 `degradedCapabilities` 中返回非敏感能力提示。

## 14. 数据库与迁移

新增表建议：

```text
project_profile
project_claim
project_probe_template
interview_plan
interview_turn
turn_evaluation
```

扩展：

```text
interview_session
interview_report
```

`interview_report` 新增：

```text
mode
generation_status              PENDING / GENERATING / COMPLETED / FAILED
report_json
schema_version
generated_at
error_code
```

保留现有总分、等级和文本列供八股报告兼容。项目深挖的结构化事实以版本化 `report_json` 为准。

第一批引入 Flyway，使用版本化迁移：

```text
V1__baseline_existing_schema.sql
V2__project_deep_dive_core.sql
V3__interview_turn_and_evaluation.sql
V4__project_probe_seed.sql
```

迁移切换步骤固定为：

1. 添加 Flyway 依赖；
2. 禁用 Spring `schema.sql` 和 `data.sql` 自动初始化；
3. 新数据库由 `V1__baseline_existing_schema.sql` 创建当前已有表；
4. 已有非空数据库启用 `baseline-on-migrate=true`、`baseline-version=1`，从 `V2` 开始升级；
5. `V2` 修改 `interview_session.module` 和 `current_question_type` 为项目模式可空，并新增项目深挖表；
6. `data.sql` 中必要的手工题卡和深挖模板迁入版本化或 Repeatable Migration；
7. `data.generated.sql` 改为显式题库导入工具，不再随应用启动执行；
8. 生产升级前备份数据库，并对比升级前后的会话、回答、报告和题卡行数；
9. 在本地旧库和全新空库各执行一次迁移验证。

不继续使用启动时反复执行大型 `data.generated.sql` 作为长期数据库演进方式。

迁移必须兼容现有八股会话和报告数据，不能直接删除旧表或旧接口。

## 15. 安全与隐私

- 项目描述输入前提示删除手机号、邮箱、身份证号和公司敏感信息；
- 后端对常见手机号、邮箱等内容进行检测和脱敏；
- 数据库只保存 `sanitized_description`，第一批不保存未脱敏原文；
- 日志不记录完整项目原文、完整回答和完整 Prompt；
- `/api/llm/test` 在生产环境关闭或限制；
- 项目档案和会话通过访问令牌进行资源归属校验；
- 外部调用增加限流；
- 未来语音只保存用户确认后的转写文本，不保存原始音频。
- 前端项目草稿仅保存在 `sessionStorage`，创建档案成功后清理；不默认长期保存在 `localStorage`。

## 16. 测试设计

### 16.1 Domain 单元测试

- 项目档案状态迁移；
- 面试状态机合法和非法迁移；
- 同一 Claim 的追问上限；
- 必须覆盖的深挖维度；
- LLM 非法决策修正；
- 报告聚合规则；
- 文字输入和语音转写输入生成相同领域命令。

### 16.2 Application 服务测试

- 创建并分析项目档案；
- 结构化 LLM 输出字段缺失和坏 JSON；
- 创建面试时保存项目和模板快照；
- 相同 `clientTurnId` 不重复调用 LLM；
- LLM 超时后会话仍可重试；
- Milvus 异常不阻断回答；
- 达到追问上限后切换维度或 Claim；
- 项目模式回答响应不泄露内部评价；
- 八股模式仍保留即时反馈；
- 资源访问令牌错误时拒绝访问。

### 16.3 集成测试

使用真实 MySQL 表结构、Stub LLM 和 Stub Retrieval 验证：

```text
创建 Profile
→ 分析 Claim
→ 用户确认
→ 创建项目面试
→ 项目总览回答
→ 指标追问
→ 技术原理追问
→ 方案取舍追问
→ 结束
→ 获取逐轮报告
```

必须验证：

- 消息顺序；
- Claim 覆盖状态；
- 评价结构化保存；
- 状态迁移；
- 报告引用真实遗漏点；
- 重复请求幂等；
- 并发请求只推进一次状态。

### 16.4 黄金项目场景

准备固定测试项目：

- Redis 缓存优化；
- JWT 登录鉴权；
- RabbitMQ 异步削峰；
- MySQL 索引优化；
- 秒杀或高并发系统。

测试使用固定 Stub 输出，不能依赖在线模型的随机结果判断 CI 是否通过。

### 16.5 前端验证

- 增加 Vitest 和 Vue Test Utils；
- TypeScript 类型检查；
- 项目草稿自动保存；
- 提取确认页状态切换；
- 面试消息渲染和断线恢复；
- 重复点击只产生一个 `clientTurnId`；
- 项目面试页面不渲染内部评价字段；
- 报告页逐轮展开；
- 桌面、平板和手机响应式布局；
- 为未来语音状态保留类型和组件边界。

## 17. 可观测性

每次 LLM 调用记录：

- `traceId`；
- `sessionId`；
- 用途：项目分析、回答评价或报告生成；
- 模型名；
- 调用耗时；
- Token 消耗；
- 重试次数；
- 是否使用降级结果。

不记录：

- API Key；
- 完整 Prompt；
- 完整项目描述；
- 完整用户回答。

业务指标：

- 项目分析成功率；
- 项目面试创建成功率；
- 回答评价成功率；
- 平均追问轮数；
- 面试完成率；
- 报告生成成功率；
- LLM 超时和坏 JSON 比例；
- Milvus 降级比例。

## 18. 从 reg-pilot 借鉴与避免

借鉴：

- 契约优先；
- 模块公开接口和单向依赖；
- 薄 Controller；
- Param、内部 DTO 和前端 VO 的边界隔离；
- 外部 Agent/LLM Client 与业务逻辑分离；
- 外部 AI 调用位于数据库事务之外；
- 统一错误码和异常处理；
- Typed API、路由闭环和轻量状态管理；
- Service 和状态机测试；
- 代码通过验证闭环后再提交。

避免：

- 全局 `zoo` 大杂烩目录；
- 巨型 ServiceImpl；
- 按角色复制多份相似页面；
- 页面直接解析 SSE；
- 手工日期迁移脚本；
- 当前规模不需要的复杂 RBAC；
- 过早拆分独立线上 Python Agent 服务。

## 19. 分阶段实施

本规格是项目深挖模块总体设计，不作为一个超大实施任务一次完成。实际开发拆成三个连续实施计划：

1. 基础模型与项目档案：Flyway、Profile、Claim、Template、确认流程和资源令牌；
2. 项目面试闭环：Plan、Turn、Evaluation、状态机、多轮追问、幂等和旧八股兼容；
3. 复盘与前端体验：报告聚合、项目设置页、沉浸式面试室、刷新恢复和前端测试。

Milvus 和语音继续使用独立后续规格。

### 阶段 0：设计与基础规则

- 本设计规格；
- 状态迁移表；
- API 契约；
- 错误码；
- ADR：模块化单体、反馈时机、Milvus 降级、语音数据策略。

### 阶段 1：持久化和共享内核骨架

- 引入 Flyway；
- 新增 Profile、Claim、Template、Plan、Turn 和 Evaluation 表；
- 扩展 Session；
- 建立错误码；
- 建立 `InterviewModePolicy` 和状态机；
- 保持现有八股接口可用。

### 阶段 2：项目档案提取

- 创建项目档案；
- 敏感信息检测；
- LLM 结构化提取；
- Claim 生成；
- 用户确认接口；
- Profile 和 Claim 测试。

### 阶段 3：项目面试最小闭环

- 生成计划快照；
- 创建 Opening Turn；
- 提交回答；
- 生成一次追问；
- 保存 Evaluation；
- 前端项目设置页和基础面试室。

### 阶段 4：真实多轮面试

- 连续追问两到三轮；
- 切换维度和 Claim；
- 自然转场；
- 幂等和并发控制；
- 页面刷新恢复；
- 隐藏内部反馈；
- 沉浸式面试布局。

### 阶段 5：个性化报告

- 按 Claim 和维度聚合；
- 逐轮复盘；
- 基于实际 MissingPoints 和 Evidence 生成建议；
- 报告页；
- 完整集成测试。

### 阶段 6：Milvus

- `apps/ingest`；
- 资料清洗和版权检查；
- Embedding；
- Milvus 灌库；
- 检索、过滤和追踪；
- 项目追问和报告增强。

### 阶段 7：语音

- 麦克风权限和录音；
- ASR；
- 用户确认最终转写；
- TTS；
- 流式传输；
- 打断、静音检测和恢复；
- 不保存原始音频。

## 20. 第一批验收标准

第一批完成标准：

- 用户可以创建、分析、修正并确认项目档案；
- 给定固定黄金项目文本，输入中存在的技术栈、职责和指标至少各提取一项，不允许编造输入中不存在的指标；
- 用户能够确认或修正提取结果；
- 用户能够发起项目深挖面试；
- 系统至少覆盖项目总览、个人职责、技术原理和方案取舍；
- 固定 Stub 场景下，同一核心 Claim 的主问之后至少产生两个针对不同证据缺口的追问，且不超过配置上限；
- 项目回答响应 JSON 中不存在 `score`、`hitPoints`、`missingPoints`、`decision` 和 `modelRawResponse`；
- 相同 `clientTurnId` 重复提交时，LLM 调用次数严格为一次；
- LLM 超时后可以安全重试；
- 进程中断形成的过期 `PROCESSING` Turn 可以恢复为可重试状态；
- 使用 `NoOpVectorRetrievalService` 时完整项目面试流程通过；
- 用户主动提前结束时，未覆盖维度标记为 `NOT_ASSESSED`；
- 面试结束后生成基于逐轮评价的项目报告，每条关键结论至少引用一个 `candidateTurnId`；
- 后端核心单元测试和集成测试通过；
- 旧 `/answers`、`/next-question` 和 `/report` 契约回归测试通过；
- 前端类型检查和生产构建通过；
- 前端 Vitest 测试通过；
- 现有八股练习仍可使用。

## 21. 完成定义

该模块只有在以下条件同时满足时才算完成：

1. 设计、契约、数据库、后端、前端和测试保持一致；
2. 真实用户可以不阅读技术说明完成项目输入、确认、面试和复盘；
3. 报告结论可以追溯到具体项目 Claim 和具体回答；
4. 现有八股功能没有回归；
5. LLM、Milvus和网络异常不会造成重复数据或不可恢复状态；
6. 后续接入 Milvus 和语音时不需要重写项目深挖领域规则。
