# 个性化训练 Agent 一期设计规格

日期：2026-08-10

状态：已确认

适用仓库：`agent2026-interview-agent`

## 1. 背景与目标

系统已经具备八股练习、项目深挖、算法口述、综合模拟、JWT 身份和统一训练历史。现状能够完成单次训练、逐轮评价和报告生成，但不同训练模块仍然彼此独立：系统不会把跨场次表现沉淀为用户能力画像，也不会基于已有证据判断下一步最值得训练的内容。

本期建设独立的 `trainingagent` 模块，形成第一条个性化训练闭环：

```text
各模块报告
→ 统一训练证据
→ 用户能力画像
→ 下一步训练推荐
→ 首页展示推荐和原因
```

一期成功的核心不是让 LLM 自主控制训练，而是让每项推荐都能追溯到真实训练证据，并且在 Agent 模块不可用时不影响现有四个训练模块。

## 2. 范围

### 2.1 本期包含

- 把八股、项目深挖和算法报告转换为统一能力证据。
- 综合模拟引用其子会话证据，不重复计数。
- 建立不可变证据账本、用户能力快照、当前推荐和同步状态。
- 使用确定性、可版本化的规则生成一项主推荐和最多两项备选训练。
- 首页展示“今天最值得练”、推荐原因和少量能力摘要。
- 推荐动作使用已有模块入口和预设参数，不自动创建训练会话。
- 对现有已完成训练进行幂等回填。
- 提供事件快速同步、首页轻量补偿和定时补偿三层恢复机制。
- 增加功能开关、自动化测试和生产回滚能力。

### 2.2 本期不包含

- 自动替用户开始训练。
- 自动改变八股选题、项目追问、算法阶段或综合模拟编排。
- 新建独立能力画像大屏、雷达图、排行榜或精确能力分展示。
- 由 LLM 决定能力状态、证据权重或最终推荐动作。
- 语音识别、TTS、实时音频和原始音频保存。
- 新增题库模块或扩大知识库内容。
- 调整 RAG/Milvus 部署结构。
- 重构四个既有模块的领域状态机。

后续二期在本期证据和画像稳定后，再接入自适应选题与个性化综合模拟。

## 3. 设计原则

1. **证据先于结论**：非冷启动推荐必须引用真实报告、轮次或评价证据。
2. **确定性决策**：在画像、最近训练序列、策略版本和评估时点相同的条件下，必须得到相同推荐。
3. **模块隔离**：Agent 只通过公开应用接口读取报告事实，不访问其他模块内部 Mapper。
4. **故障隔离**：Agent 同步、聚合或推荐失败不能回滚已经完成的训练和报告。
5. **可重建**：证据来源于原始报告，画像和推荐均为可重新计算的派生数据。
6. **不制造精确感**：内部允许数值计算，用户界面只展示有限状态和自然语言原因。
7. **保持轻量**：首页突出一项动作，原有四个模块入口继续保留。

## 4. 总体架构

```text
knowledge / projectdeepdive / algorithmpractice
                    │
                    │ TrainingCompletedEvent
                    ▼
trainingagent.infrastructure.source
├── KnowledgeEvidenceAdapter
├── ProjectEvidenceAdapter
├── AlgorithmEvidenceAdapter
└── SimulationEvidenceAdapter（只解析子会话引用）
                    │
                    ▼
trainingagent.application
├── EvidenceSynchronizationService
├── AbilityProfileApplicationService
├── TrainingRecommendationApplicationService
└── TrainingAgentQueryService
                    │
                    ▼
trainingagent.domain
├── AbilityEvidence
├── AbilityDimension
├── AbilityProfileAggregator
├── TrainingRecommendationPolicy
└── RecommendationReason
                    │
                    ▼
trainingagent.infrastructure.persistence
├── MyBatis evidence repository
├── MyBatis snapshot repository
├── MyBatis recommendation repository
└── MyBatis sync-state repository
```

依赖方向为：

```text
API → Application → Domain
                  ↑
       Infrastructure 实现端口
```

`trainingagent` 可以依赖各训练模块公开的报告查询用例和只读 DTO，但不得依赖其持久化实体、Mapper 或内部事务。各训练模块只发布位于共享契约中的 `TrainingCompletedEvent`，不依赖 `trainingagent` 的具体实现。

## 5. 组件职责

### 5.1 训练完成事件

事件包含：

```text
TrainingCompletedEvent
- userId
- sourceType
- sourceSessionId
- sourceReportVersion
- completedAt
```

事件在报告事务提交后处理。发布事件的模块不等待画像重算结果，也不因 Agent 处理失败而改变报告状态。

当前各模块报告采用 `generateIfAbsent`，因此 `sourceReportVersion` 初始固定为 `1`。只有来源模块未来真正持久化了新的报告修订版时才递增；它不等同于报告 JSON 的 schema 版本。

### 5.2 证据适配器

每个适配器负责把模块公开报告转换为统一证据，不负责持久化和推荐：

- 八股适配器读取会话模块、逐题命中点、遗漏点、薄弱点和报告结论。
- 项目适配器读取探查维度、声明、风险、轮次证据和报告结论。
- 算法适配器读取阶段评价、正确性、优化、复杂度、边界和表达证据。
- 综合模拟适配器只解析子会话 ID，并触发子报告同步；综合结论不得复制成第二份能力证据。

适配器只能输出受控能力维度。无法映射的文本记录为同步异常，不得让 LLM 临时创建新维度。

### 5.3 能力画像聚合器

聚合器以不可变证据为输入，按用户和能力维度生成当前快照。它不读取前端状态，不调用 LLM，也不修改原报告。

### 5.4 推荐策略

推荐策略根据能力快照、最近训练模块和证据时间生成：

- 一项主推荐；
- 最多两项不同训练方向的备选推荐；
- 推荐原因码和证据引用；
- 模块、目标维度和可调整的预设参数。

### 5.5 Dashboard 查询服务

查询服务负责：

1. 对当前用户执行一次有上限的轻量补偿；
2. 读取能力摘要和有效推荐；
3. 在推荐缺失或过期时重算；
4. 返回不含内部精确分的首页 DTO。

## 6. 能力维度

能力维度使用稳定代码和中文展示名。数据库、策略和测试使用稳定代码，前端只消费展示名。

| 类别 | 稳定代码 | 展示名 |
|---|---|---|
| 八股 | `KNOWLEDGE.JAVA` | Java |
| 八股 | `KNOWLEDGE.MYSQL` | MySQL |
| 八股 | `KNOWLEDGE.REDIS` | Redis |
| 八股 | `KNOWLEDGE.SPRING` | Spring |
| 八股 | `KNOWLEDGE.NETWORK` | 计算机网络 |
| 八股 | `KNOWLEDGE.OS` | 操作系统 |
| 项目 | `PROJECT.OWNERSHIP` | 个人贡献 |
| 项目 | `PROJECT.AUTHENTICITY` | 真实性 |
| 项目 | `PROJECT.PRINCIPLE` | 技术原理 |
| 项目 | `PROJECT.TRADEOFF` | 方案取舍 |
| 算法 | `ALGORITHM.CORRECTNESS` | 思路正确性 |
| 算法 | `ALGORITHM.OPTIMIZATION` | 优化推导 |
| 算法 | `ALGORITHM.COMPLEXITY` | 复杂度分析 |
| 算法 | `ALGORITHM.EDGE_CASE` | 边界意识 |
| 算法 | `ALGORITHM.COMMUNICATION` | 算法表达结构 |
| 通用 | `GENERAL.ANSWER_STRUCTURE` | 表达完整性 |
| 通用 | `GENERAL.EVIDENCE` | 证据意识 |

题卡标签、算法标签和项目声明仍作为证据元数据保存，但一期不动态扩展画像维度，避免标签基数失控。

同一条回答可以同时形成一个模块维度和一个通用维度的证据，但每个维度独立聚合，不能把两者相加成虚构的总分。

## 7. 数据模型

### 7.1 `training_ability_evidence`

不可变证据账本：

```text
id
user_id
source_type                  KNOWLEDGE / PROJECT_DEEP_DIVE / ALGORITHM
source_session_id
source_report_id             可空；没有独立报告主键时使用会话和版本定位
source_report_version
evidence_key
dimension_code
polarity                     STRENGTH / GAP / RISK
severity                     1..5
confidence                   0..1
evidence_text
source_turn_id
source_evaluation_id
metadata_json
observed_at
create_time
```

唯一约束：

```text
(source_type, source_session_id, source_report_version, evidence_key)
```

`evidence_text` 只保存支持推荐所需的短摘要，不复制完整项目描述或整段回答。`metadata_json` 只允许受控的模块、难度、题卡标签、项目声明 ID 和算法标签。

### 7.2 `user_ability_snapshot`

每个用户、每个能力维度一行：

```text
user_id
dimension_code
ability_state                UNKNOWN / NEEDS_WORK / DEVELOPING / STABLE / STRONG
internal_value
confidence
strength_count
gap_count
risk_count
distinct_session_count
last_observed_at
aggregation_policy_version
version
update_time
```

唯一约束：

```text
(user_id, dimension_code)
```

`internal_value` 只用于后端排序和状态判断，不进入公开 Dashboard DTO。

### 7.3 `training_recommendation`

每个用户保存一个当前推荐快照：

```text
user_id
recommendation_revision
dashboard_state              COLD_START / READY
primary_training_type
primary_dimension_code
primary_title
primary_reason
primary_action_json
alternative_items_json
evidence_ids_json
recommendation_policy_version
generated_at
expires_at
version
```

使用 `user_id` 作为唯一键，以乐观锁覆盖当前推荐。新证据生成后使当前推荐失效；没有新证据时，同一推荐最多有效 24 小时。

### 7.4 `training_evidence_sync_state`

技术补偿状态：

```text
source_type
source_session_id
source_report_version
user_id
status                       PENDING / COMPLETED / FAILED / REJECTED
attempt_count
next_retry_at
last_error_code
last_attempt_at
update_time
```

唯一约束：

```text
(source_type, source_session_id, source_report_version)
```

同步状态不保存 API Key、完整报告、完整异常堆栈或敏感项目文本。

`FAILED` 表示依赖暂时不可用等可重试错误；`REJECTED` 表示未知维度、归属缺失或报告损坏等需要代码或数据修复的错误，其 `next_retry_at` 为空，不进入自动重试循环。

## 8. 证据聚合规则

### 8.1 单条证据贡献

每条证据使用下式计算内部贡献：

```text
polarityFactor × severity × confidence × recencyFactor
```

初始系数：

| 项目 | 系数 |
|---|---:|
| `STRENGTH` | `+1.00` |
| `GAP` | `-1.00` |
| `RISK` | `-1.25` |
| 7 天内 | `1.00` |
| 8 至 30 天 | `0.85` |
| 31 至 90 天 | `0.60` |
| 90 天以上 | `0.35` |

同一会话、同一维度的累计绝对贡献设置上限，避免一次长对话压过多个独立场次。初始上限为 `5.0`。

### 8.2 快照状态

聚合值 `netValue` 为该维度全部受限贡献之和。聚合可信度按不同场次数和证据可信度共同计算，并限制在 `0..1`。

状态规则按顺序匹配：

1. 没有证据：`UNKNOWN`。
2. 最近 30 天存在 `severity >= 4` 且 `confidence >= 0.7` 的风险，或 `netValue <= -2`：`NEEDS_WORK`。
3. 不同场次至少 3、`netValue >= 5`、聚合可信度至少 `0.70`，并且最近 30 天没有 GAP 或 RISK：`STRONG`。
4. 不同场次至少 2、`netValue >= 2` 且聚合可信度至少 `0.55`：`STABLE`。
5. 其余有证据的情况：`DEVELOPING`。

`STRONG` 的判断优先级低于 `NEEDS_WORK`，因此新出现的高风险可以立即降低原有强项状态。

## 9. 推荐策略

### 9.1 候选优先级

推荐候选按以下优先组依次选择：

1. 最近高可信、高严重度的风险。
2. 来自至少两个不同场次的重复 GAP。
3. `NEEDS_WORK` 维度。
4. 尚未观察的核心维度。
5. 超过 90 天没有新证据的维度。
6. `STABLE` 维度的保持训练。

`STRONG` 维度不生成主推荐，除非其他维度均无候选。

### 9.2 同组排序

同一优先组内部使用：

```text
severity
× confidence
× recencyFactor
× recurrenceFactor
× coreDimensionWeight
× fatigueFactor
```

初始规则：

- `recurrenceFactor = 1 + min(0.5, 0.15 × (distinctSessionCount - 1))`。
- 项目四个维度、Java、MySQL、算法正确性和复杂度的 `coreDimensionWeight = 1.15`，其他维度为 `1.00`。
- 最近连续两次训练都属于同一训练类型时，该类型 `fatigueFactor = 0.75`，否则为 `1.00`。
- 高可信、高严重度风险的 `fatigueFactor` 固定为 `1.00`。
- 单一训练类型最多占主推荐和备选中的一个位置，避免三个推荐都指向同一模块。

### 9.3 冷启动

用户没有任何证据时固定返回：

```text
Java 核心 · 3 题快速校准
难度：混合
原因：完成首次校准后，系统才能依据真实表现安排训练。
```

冷启动不写入虚构画像证据。

### 9.4 训练动作映射

- 八股：提供模块、难度和题数预设，用户进入页面后仍需点击开始。
- 项目：提供目标项目维度和最近相关的已确认项目档案，用户可以更换项目。
- 算法：提供目标能力维度、建议难度和标签过滤，用户自行选择题目。
- 通用表达：根据相关证据最多的来源模块选择八股、项目或算法入口。
- 一期不把综合模拟作为首选补弱动作；它可以在主要维度较稳定时作为保持训练的备选。

### 9.5 推荐理由

理由由固定模板、维度展示名、不同场次数和短证据摘要构成：

```text
最近两次算法口述都遗漏了空间复杂度分析，建议完成一题中等难度算法口述，重点覆盖复杂度和边界。
```

LLM 一期不参与理由生成。后续如接入 LLM，只允许在不改变事实、证据引用和训练动作的条件下润色文本。

## 10. 同步、补偿与事务

### 10.1 快速路径

```text
报告事务提交
→ AFTER_COMMIT 事件监听
→ 创建或领取 sync state
→ 适配报告
→ 批量幂等写证据
→ 重算受影响维度
→ 使旧推荐失效并生成新推荐
→ 标记 sync state COMPLETED
```

外部 LLM 调用不属于 Agent 同步流程。同步过程不占用报告生成事务。

### 10.2 首页轻量补偿

Dashboard 查询只检查当前用户最近的已完成报告，最多补偿 10 个未同步版本。超过上限的内容交给定时任务，避免首页请求变成长事务。

### 10.3 定时补偿

定时任务扫描 `PENDING` 和到达 `next_retry_at` 的 `FAILED` 状态，按指数退避重试。单次任务有批量上限，同一来源通过唯一键和领取状态避免并发重复处理。`REJECTED` 只在代码、映射或源数据修复后由受控重建流程重新处理。

无法识别的维度、缺失归属或损坏报告使用稳定错误码记录，并标记为 `REJECTED`，不生成猜测证据。

### 10.4 重新计算

聚合或推荐策略版本变化时，后台可从证据账本重建全部快照和推荐，不需要重新调用 LLM，也不修改原报告。

## 11. API 设计

### 11.1 Dashboard

```http
GET /api/training-agent/dashboard
Authorization: Bearer <access-token>
```

响应遵守现有 `Result<T>` 包装：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "state": "READY",
    "focusDimensions": [
      {
        "dimensionCode": "ALGORITHM.COMPLEXITY",
        "label": "复杂度分析",
        "abilityState": "NEEDS_WORK",
        "evidenceCount": 3,
        "lastObservedAt": "2026-08-10T10:00:00"
      }
    ],
    "primaryRecommendation": {
      "revision": 4,
      "trainingType": "ALGORITHM",
      "title": "练一题中等难度算法口述",
      "reason": "最近两次算法口述都遗漏了空间复杂度分析。",
      "estimatedMinutes": 15,
      "action": {
        "dimensionCode": "ALGORITHM.COMPLEXITY",
        "difficulty": "medium"
      },
      "evidenceCount": 2
    },
    "alternatives": [],
    "generatedAt": "2026-08-10T10:01:00"
  }
}
```

公开响应不包含 `internalValue`、内部优先值、完整回答或其他用户数据。前端根据 `trainingType` 和语义化 `action` 映射到现有路由，后端不硬编码前端 URL。

### 11.2 错误行为

- 未登录：沿用现有 JWT 认证错误。
- Agent 功能关闭：返回明确的功能关闭状态，前端展示原首页。
- 短暂同步失败：优先返回最后一个未过期快照；没有快照时返回服务暂不可用，前端降级。
- 画像为空：返回 `COLD_START`，不是错误。

一期不提供修改画像、手工改分、删除单条证据或强制推荐的公开接口。

## 12. 首页体验

首页结构调整为：

```text
欢迎语
今天最值得练
├── 建议训练
├── 预计用时
├── 最多两条证据原因
└── 进入训练

当前训练重点
├── 最多三个待补强或发展中维度
└── 一项最近取得的进展

原有四个模块入口
├── 八股练习
├── 项目深挖
├── 算法口述
└── 综合模拟
```

体验约束：

- 不展示雷达图、排行榜和精确能力分。
- 不新增独立画像页面。
- 不在沉浸式面试过程中展示画像、分数、命中点或系统动作。
- 主推荐按钮只打开带预设参数的现有模块页面。
- 用户可以忽略推荐并选择任意模块。
- Dashboard 请求失败时，推荐区域显示简短降级提示，四个模块入口正常可用。

## 13. 历史数据与上线

### 13.1 数据库迁移

新增 Flyway `V9` 迁移，创建四张表、索引、唯一约束和外键。迁移只新增结构，不修改或删除原训练数据。

`user_id` 可以关联 `app_user`；不同训练类型的 `source_session_id` 属于多态引用，不建立指向单一业务表的外键，由适配器和用户归属检查保证有效性。

### 13.2 历史回填

- 按用户和完成时间分批读取已完成报告。
- 每批数量有上限，使用同步状态和证据唯一键保证可中断、可重试。
- 综合模拟只解析子会话引用。
- 缺少 `user_id` 的历史兼容数据不自动归属新用户，也不进入其画像。
- 训练历史的“隐藏”仍只影响列表可见性，不删除原报告或能力证据。未来若增加真正的数据删除能力，必须同步清除相关证据并重建画像。

### 13.3 功能开关

增加：

```text
TRAINING_AGENT_ENABLED=true|false
```

默认启用；出现问题时可以关闭 Agent Dashboard，首页立即退回原有模块入口，四个训练模块继续工作。

### 13.4 可观察性

结构化日志和内部指标至少包括：

- 待同步、成功和失败报告数量；
- 同步延迟；
- 幂等冲突数量；
- 画像重算耗时；
- Dashboard 降级次数；
- 冷启动和正常推荐数量。

日志不得记录完整 JWT、Refresh Cookie、API Key、完整项目描述或完整候选人回答。

## 14. 测试设计

### 14.1 Domain 单元测试

- 无证据生成 `UNKNOWN` 和冷启动推荐。
- 单场强项证据仍为 `DEVELOPING`，避免过早认定稳定。
- 两个不同场次的重复 GAP 提升推荐优先级。
- 新的高风险覆盖旧的 `STRONG` 状态。
- 疲劳惩罚降低重复模块，但不能压过高风险。
- 单场贡献上限生效。
- 证据时间衰减、推荐过期和策略版本重算正确。
- 主推荐和备选不重复训练类型。

### 14.2 适配器契约测试

- 八股命中点、遗漏点和薄弱点映射到合法维度。
- 项目四个探查维度、风险和声明引用映射正确。
- 算法六阶段评价映射到五个算法维度。
- 综合模拟只返回子会话引用。
- 未知维度和损坏报告失败，不生成自由标签或猜测证据。

### 14.3 MySQL 集成测试

- 同一报告版本同步任意次数只产生一组证据。
- 并发同步只有一个处理者完成有效写入。
- 证据、快照、推荐和同步状态的归属均按 `user_id` 隔离。
- 失败补偿、过期领取和历史回填可重试。
- 策略版本变化可以从账本重建快照。

### 14.4 API 测试

- JWT 缺失、过期和用户隔离。
- 冷启动、正常推荐、过期推荐重算和最后快照降级。
- 响应不暴露内部数值、完整回答和其他用户证据。
- 功能开关关闭时行为明确。

### 14.5 前端测试

- 冷启动卡片。
- 主推荐、备选推荐和能力摘要。
- 推荐动作正确映射到现有模块参数。
- Dashboard 失败时四个模块入口仍可使用。
- 页面不渲染内部能力分。

### 14.6 端到端测试

1. 新用户看到 Java 三题冷启动建议。
2. 完成一次训练后，下一次首页访问出现基于证据的新推荐。
3. 两个场次出现同一薄弱点后，该维度优先级上升。
4. 高风险证据不被疲劳惩罚压低。
5. 完成综合模拟后，子会话证据只计一次。
6. 关闭 Agent 功能后，现有四模块仍可完整训练。

## 15. 验收标准

- 每项非冷启动推荐至少引用一条真实能力证据。
- 同一报告版本同步任意次数，证据数量不增加。
- 在画像、最近训练序列、策略版本和评估时点相同时生成相同推荐。
- 完成训练后，下一次首页访问能够看到推荐更新。
- 用户之间的证据、画像和推荐完全隔离。
- 综合模拟不会重复累计子会话证据。
- Agent 同步和 Dashboard 故障不影响既有训练与报告。
- 首页只突出一项推荐，不显示内部能力分。
- 后端、前端自动化测试和生产构建全部通过。
- 正常已有快照的 Dashboard 查询不执行全量报告扫描。

## 16. 实施拆分

### 阶段一：领域与数据基础

- V9 数据库迁移。
- 能力维度、证据、快照和推荐领域模型。
- 聚合策略和推荐策略单元测试。

### 阶段二：模块证据接入

- 训练完成事件。
- 八股、项目和算法适配器。
- 综合模拟子会话去重。
- 同步状态、幂等写入和补偿任务。

### 阶段三：Dashboard 与首页

- Dashboard API 和 JWT 隔离。
- 首页主推荐、训练重点和降级体验。
- 现有模块预设参数接入。

### 阶段四：回填与交付

- 历史报告幂等回填。
- MySQL、前端和端到端测试。
- 功能开关、日志与生产部署验证。

每个阶段必须独立通过对应测试后再进入下一阶段。二期自适应选题和自动综合模拟不进入本实施计划。
