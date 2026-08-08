# 基础模块补全设计规格

日期：2026-08-08
状态：已确认，待实施
适用仓库：`agent2026-interview-agent`

## 1. 背景与目标

当前系统已经具备八股练习和项目深挖两个可运行模块，但缺少稳定用户身份、算法口述、统一训练历史和综合模拟面试。继续建设知识库、语音或部署优化之前，先补齐这些基础模块，使产品形成完整的 Java 技术面试训练主干。

本阶段目标：

1. 建立基于双 JWT 的站内账号体系，让训练数据具有稳定用户归属。
2. 建立算法口述独立训练闭环，训练面试中的思路表达而非在线判题。
3. 建立统一训练历史，聚合不同业务模块的训练入口和报告入口。
4. 建立综合模拟面试编排，将项目深挖、八股和算法组合成一场完整技术面试。
5. 保持现有八股和项目深挖业务策略不变，通过公开用例接口渐进接入。

## 2. 本阶段范围

### 2.1 包含

- 用户名和密码注册、登录、刷新、退出、获取当前用户。
- Access JWT 与 Refresh JWT 的签发、轮换、撤销和重放检测。
- 八股、项目深挖、算法和综合模拟数据的用户归属与访问隔离。
- 算法题、算法训练会话、多轮口述、逐轮评价和证据化报告。
- 统一训练历史的列表、筛选、继续训练、查看报告和隐藏记录。
- 综合模拟的阶段计划、阶段切换、异常恢复和综合报告。
- 前后端自动化测试、数据库迁移、回归验证和正式部署。

### 2.2 不包含

- 短信、邮箱验证、找回密码、第三方登录和天津大学统一认证。
- 管理后台、角色权限系统和组织架构。
- 在线代码编辑、代码执行、测试用例判题和竞赛排名。
- 操作系统、计算机网络等新八股题库模块。
- Milvus、资料灌库和向量检索增强。
- 语音识别、TTS、实时音频和原始音频保存。
- 蓝绿部署、监控平台、域名和 HTTPS 基础设施改造。

## 3. 实施策略

采用独立垂直模块逐步交付：

```text
登录与用户归属
→ 算法口述
→ 统一训练历史
→ 综合模拟面试
→ 全链路回归与部署
```

不先重构完整通用面试内核，也不将三个新能力硬编码到一个综合页面。每个模块必须能够独立运行和测试，综合模拟只负责编排，不复制子模块业务逻辑。

## 4. 总体模块架构

```text
Java 面试训练平台
├── identity                 # 新增：账号、认证和登录会话
├── knowledgepractice        # 已有：轻量八股练习
├── projectdeepdive          # 已有：沉浸式项目面试
├── algorithmpractice        # 新增：算法口述训练
├── interviewsimulation      # 新增：综合模拟编排
└── traininghistory          # 新增：统一历史索引
```

后端新模块继续遵守：

```text
API → Application → Domain
                  ↑
          Infrastructure 实现端口
```

约束：

- Domain 不依赖 Spring MVC、MyBatis、JWT 库或具体 LLM 客户端。
- API 层只处理协议、认证主体和参数校验。
- Application 层编排用例、短事务和跨模块公开接口。
- Infrastructure 实现持久化、密码哈希、JWT 和外部模型适配。
- 综合模拟不得读取八股、项目或算法模块内部 Mapper。

## 5. 身份与认证模块

### 5.1 功能范围

第一版提供：

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

用户名是第一版唯一登录标识。用户名规范化后唯一，密码必须满足最低长度和基础复杂度要求。密码仅保存 BCrypt 哈希，不记录明文、可逆密文或认证请求体。

### 5.2 双 JWT 模型

- Access Token 有效期 15 分钟，只由前端保存在内存中。
- Access Token 通过 `Authorization: Bearer <token>` 发送。
- Refresh Token 有效期 30 天，保存在 `HttpOnly` Cookie 中。
- 生产 Cookie 使用 `Secure`，同源场景使用合适的 `SameSite` 策略。
- Refresh Token 每次使用后立即轮换，旧 Token 失效。
- 数据库只保存 Refresh Token 的强哈希、JTI、token family、过期和撤销状态。
- JWT 密钥从生产环境变量读取，不进入仓库、日志或前端产物。

### 5.3 刷新和重放检测

刷新成功时，在同一 token family 中创建后继会话并撤销旧会话。如果已经轮换的旧 Refresh Token 再次出现，视为可能泄露，撤销同一 token family 的全部有效会话并要求重新登录。

退出登录撤销当前 Refresh 会话。用户被禁用或修改密码时撤销该用户全部 Refresh 会话。本阶段不提供修改密码页面，但领域能力需要允许后续接入。

### 5.4 前端认证状态

页面加载后调用刷新接口恢复 Access Token，再获取当前用户。多个请求同时收到 401 时只能共享一次刷新请求：

```text
多个 401
→ 单一 refresh Promise
→ 成功后分别重放一次原请求
→ 再次失败则清空身份并进入登录页
```

Access Token 和 Refresh Token 均不得写入 `localStorage` 或 `sessionStorage`。前端只允许保存非敏感的界面偏好。

### 5.5 CSRF 与 XSS 边界

- 普通业务 API 使用 Authorization Header，不自动携带认证 Cookie。
- Refresh 和 Logout 接口会携带 Cookie，必须校验 `Origin`，并限制为同源请求。
- Cookie 限定路径，减少无关请求携带范围。
- 所有用户文本按普通文本渲染，不使用未清洗的 `v-html`。
- 登录、注册和刷新接口使用独立限速规则。

## 6. 用户归属与兼容策略

八股会话、项目档案、项目面试、算法会话、综合模拟和训练历史均需要稳定 `user_id`。

访问规则：

- 新会话必须由已登录用户创建。
- 读取、修改、提交回答、结束和查看报告都必须验证当前用户归属。
- 不允许仅凭资源 ID 访问其他用户数据。
- 后端从认证主体取 `user_id`，不接受前端在业务请求中指定所有者。

兼容规则：

- 原有数据不自动归属任何新账号，避免错误绑定。
- 项目深挖已有资源令牌只用于兼容 `user_id` 为空的旧档案和旧会话。
- 新项目档案和会话写入 `user_id`，已登录所有者可以仅凭 JWT 跨设备访问，不再依赖保存在原浏览器中的资源令牌。
- 对 `user_id` 为空的旧数据，JWT 不会自动取得所有权，仍必须通过原资源令牌访问。
- 旧接口路径尽量保持不变，认证和归属校验在应用服务边界增加。
- 数据库迁移中的新增 `user_id` 首先允许历史空值，新数据由应用保证非空。

## 7. 算法口述模块

### 7.1 产品定位

算法口述训练面向 Java 技术面试，不建设在线判题系统。目标是让用户能够清晰完成：

1. 澄清题意和约束；
2. 给出基础方案；
3. 推导优化方案；
4. 分析时间和空间复杂度；
5. 覆盖关键边界；
6. 应对一到两次变体追问。

面试过程中不展示分数、评分命中点或系统动作，结束后统一复盘。

### 7.2 领域对象

```text
AlgorithmProblem
- id
- title
- statement
- difficulty
- tags
- constraints
- evaluation_rubric
- follow_up_templates
- status

AlgorithmSession
- id
- user_id
- problem_id
- status
- current_stage
- version
- started_at
- finished_at

AlgorithmTurn
- id
- session_id
- role
- stage
- content
- client_turn_id
- input_modality
- created_at

AlgorithmTurnEvaluation
- turn_id
- correctness_evidence
- complexity_evidence
- boundary_evidence
- communication_evidence
- missing_points
- next_action
```

第一批题目由受控迁移或幂等种子脚本写入，题卡必须包含稳定约束、评价规则和追问边界，LLM 不得自行改写题意或决定最终状态。

### 7.3 状态机

会话状态：

```text
CREATED → IN_PROGRESS → FINISHED
                      ↘ ABANDONED
```

口述阶段：

```text
CLARIFY
→ BASELINE_SOLUTION
→ OPTIMIZATION
→ COMPLEXITY
→ EDGE_CASE
→ FOLLOW_UP
→ FINISHED
```

阶段可以根据回答质量合并或跳过，但只能由后端策略在允许的状态转移内决定。每次回答仍采用 `clientTurnId` 幂等和版本并发控制，外部 LLM 调用不占用数据库长事务。

### 7.4 报告

算法报告按以下维度聚合：

- 思路正确性；
- 优化推导；
- 复杂度分析；
- 边界意识；
- 表达结构；
- 追问应对。

报告优先展示对话证据、遗漏和下一次训练建议。总分可以作为次要汇总信息，但不在面试过程中出现。

## 8. 统一训练历史

训练历史是索引模块，不复制原始对话和完整报告。

```text
TrainingHistoryEntry
- id
- user_id
- training_type
- business_session_id
- title
- status
- summary
- started_at
- finished_at
- hidden_at
```

`training_type` 第一版支持：

```text
KNOWLEDGE_PRACTICE
PROJECT_DEEP_DIVE
ALGORITHM_PRACTICE
COMPREHENSIVE_SIMULATION
```

列表支持类型、状态和时间排序。未完成记录进入对应模块继续训练，已完成记录进入原模块报告。隐藏操作仅设置 `hidden_at`，不物理删除原始面试证据。

历史写入由各业务模块在会话创建、状态变化和报告完成时通过公开端口触发。历史写入失败不能破坏面试主事务，允许幂等补偿和按业务会话重新构建索引。

## 9. 综合模拟面试

### 9.1 定位

综合模拟模拟一场完整 Java 技术一面，默认 15 至 25 分钟：

```text
开场
→ 项目深挖
→ 八股问题
→ 算法口述
→ 自然收尾
→ 综合报告
```

第一版阶段顺序固定，不提供自由拖拽、动态增删模块或由 LLM 自主改流程。

### 9.2 编排模型

```text
SimulationSession
- id
- user_id
- status
- current_stage
- project_profile_id
- version
- started_at
- finished_at

SimulationStage
- id
- simulation_session_id
- stage_type
- business_session_id
- status
- sequence
- started_at
- finished_at
```

综合模块创建阶段计划，调用子模块公开用例创建对应业务会话，并记录关联 ID。子模块仍拥有自己的轮次、评价和报告。综合模块只维护阶段状态和总报告。

### 9.3 状态与恢复

```text
CREATED
→ PROJECT
→ KNOWLEDGE
→ ALGORITHM
→ REPORTING
→ FINISHED
```

每个阶段完成后使用短事务推进状态。刷新页面时根据综合会话和当前子会话恢复。阶段创建和推进必须幂等，不能因为网络重试创建重复子会话。

单个回答失败时停留在当前阶段；已完成阶段不会回滚。用户可以主动结束，系统使用已有证据生成不完整但可解释的综合报告。

### 9.4 综合报告

综合报告引用各子报告和关键轮次，展示：

- 项目表达和真实性；
- Java 核心知识掌握；
- 算法思路与复杂度意识；
- 跨模块共性薄弱点；
- 下一轮最优训练顺序。

综合模块不得覆盖子模块原始评价，也不得根据缺失阶段伪造结论。

## 10. 页面与交互

新增页面：

```text
/login
/register
/practice/algorithm
/algorithm/:sessionId
/algorithm/:sessionId/report
/history
/simulation/new
/simulation/:sessionId
/simulation/:sessionId/report
```

路由策略：

- 登录和注册页面公开。
- 训练、历史和报告页面需要登录。
- 未登录访问受保护页面时记录目标地址，登录后返回。
- 登录用户访问登录页时回到首页。

体验区分：

- 八股继续使用轻量练习界面和即时反馈。
- 项目深挖继续使用沉浸式面试和结束后复盘。
- 算法使用专注型分阶段口述界面，不提供代码编辑器。
- 综合模拟使用统一面试室，在自然转场中切换阶段，不暴露内部编排动作。
- 历史页面是紧凑列表或表格，不做装饰性仪表盘。

## 11. 数据与事务

- 注册、刷新轮换、会话创建和状态推进使用短事务。
- LLM 调用始终位于数据库事务外。
- 用户归属校验在调用 LLM 之前完成。
- 所有轮次提交使用客户端幂等 ID。
- 所有会话状态推进使用版本号或等效乐观锁。
- 历史索引以 `(training_type, business_session_id)` 建立唯一约束。
- JWT 时间判断统一使用可注入时钟，便于测试过期边界。

## 12. 错误与安全

需要增加明确错误码：

- 用户名已存在；
- 用户名或密码错误；
- 账号已禁用；
- Access Token 无效或过期；
- Refresh Token 无效、过期、撤销或检测到重放；
- 资源不属于当前用户；
- 算法会话状态冲突；
- 综合阶段状态冲突；
- 历史索引暂时不可用。

安全日志可以记录用户 ID、会话 ID、JTI 和失败类型，但不得记录密码、完整 JWT、Refresh Cookie、API Key 或完整敏感项目描述。

## 13. 测试与验收

### 13.1 身份认证

- 注册成功、用户名规范化冲突和弱密码拒绝。
- BCrypt 哈希验证，数据库中不存在明文密码。
- Access JWT 正常、过期、篡改、错误签发方和错误受众。
- Refresh 轮换、退出撤销、过期和重放检测。
- 并发刷新只有一个后继令牌有效。
- 登录、刷新限速和日志脱敏。

### 13.2 用户隔离

- 用户 A 无法读取或操作用户 B 的任何训练、报告和历史。
- 前端提交伪造 `user_id` 不影响服务端所有者判断。
- 旧无归属数据不会出现在新用户历史中。

### 13.3 算法口述

- 至少准备 10 道覆盖数组、链表、哈希、栈队列、二叉树和基础动态规划的题目。
- 能完成澄清、方案、优化、复杂度、边界和追问闭环。
- 重复提交不会产生重复轮次。
- 报告中的证据能够追溯到真实对话。

### 13.4 训练历史

- 四种训练类型能够创建和更新历史索引。
- 筛选、继续、报告跳转和隐藏正确。
- 历史写入失败后能够幂等补偿，不损坏原业务会话。

### 13.5 综合模拟

- 项目、八股、算法阶段按顺序完成。
- 刷新、超时、重复请求和中途结束能够恢复。
- 子会话不会重复创建。
- 综合报告只引用已经完成的真实证据。

### 13.6 回归与交付

- 现有八股和项目深挖后端测试通过。
- 前端 Vitest、类型检查和生产构建通过。
- Flyway 从新数据库和现有数据库升级均通过。
- 至少使用两个账号完成跨用户隔离的端到端验收。
- 至少完成一场完整综合模拟并检查报告。
- 检查未跟踪文件和敏感信息，只提交本阶段正式文件。
- 推送 GitHub 自动部署分支后检查 Actions 和生产健康状态。
- 学校 GitLab 只有在凭证可用且推送成功时才报告已同步。

## 14. 交付拆分

### 阶段一：身份与归属

完成数据库迁移、认证后端、前端登录态、路由保护和现有会话用户归属。

### 阶段二：算法口述

完成算法题卡、状态机、多轮面试、报告和独立前端闭环。

### 阶段三：统一历史

完成历史索引、各模块接入、列表筛选、继续训练和报告跳转。

### 阶段四：综合模拟

完成阶段编排、恢复、综合报告和统一面试体验。

每个阶段必须独立通过测试和构建后再进入下一阶段，避免把账号、算法和综合编排同时留在不可运行状态。
