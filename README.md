# agent2026-interview-agent

## 项目简介

`agent2026-interview-agent` 是面向天津大学学生就业实习场景的 Java 后端技术面试训练智能体。

项目目标不是做一个普通问答机器人，而是模拟真实 Java 后端技术面试流程，围绕用户回答进行追问、评分和复盘，帮助准备软件岗实习、秋招和春招的同学系统训练面试能力。

中文产品名暂定为：**北洋 Java 面试官**。

## 参赛信息

本项目用于参加“实事求智-创见未来”天津大学 AI 智能体大赛技术实现赛道。

技术实现赛道要求注意：

- 仓库名使用 `agent2026-` 前缀。
- 以天津大学 GitLab 作为正式提交平台。
- 所有大模型调用走比赛分配的专属 `tju-llm` API 地址。
- API Key 不得提交到代码仓库。
- 开发完成后学校 GitLab 仓库应设置为内部可见，方便校内用户体验和评价。

## 核心功能规划

### Java 核心知识面试

围绕 Java 后端高频知识进行模拟面试，不是简单刷题。

首发模块包括：

- Java 基础与集合
- JVM
- Java 并发
- MySQL
- Redis
- Spring / Spring Boot
- 计算机网络
- 操作系统

系统会根据用户回答决定是否继续追问、提示、换题或结束当前问题。

### 项目经历深挖

用户可以粘贴自己的项目经历或简历片段，系统根据技术栈、个人职责和项目亮点进行追问。

示例追问方向：

- 为什么使用某个技术组件？
- 数据库表结构如何设计？
- 缓存和数据库如何保持一致？
- 如果访问量上升，系统瓶颈在哪里？
- 项目中哪些部分由用户本人完成？

### 算法思维面试

第一版不做在线判题系统，而是做面试口述型算法训练。

系统关注：

- 解题思路
- 时间复杂度
- 空间复杂度
- 边界情况
- Java 代码或伪代码表达

### 综合模拟面试

组合项目经历、Java 核心知识和算法思维，模拟一场完整 Java 后端技术一面。

### 结构化复盘报告

每次训练结束后生成复盘报告，包括：

- 总分
- 各模块分数
- 答得好的点
- 漏掉的关键点
- 面试风险表达
- 下一轮训练建议
- 推荐补强题目

## 技术栈

本项目确定使用：

- 后端：Spring Boot
- 前端：Vue 3 + TypeScript
- 数据库：MySQL
- 向量库：Milvus
- 大模型：比赛专属 `tju-llm` API

后端统一代理模型请求，前端不直接接触 API Key。

## API 配置

本项目调用比赛专属 `tju-llm` API。实际开发时在本地创建 `.env` 文件，参考 `.env.example`：

```env
TJU_LLM_API_URL=https://ai.tju.edu.cn/api/agent2026/gitlab-42-agent2026-interview-agent/chat/completions
TJU_LLM_API_KEY=replace_with_your_api_key
TJU_LLM_MODEL=tju-llm

MYSQL_URL=jdbc:mysql://localhost:3306/agent2026_interview_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=agent2026_user
MYSQL_PASSWORD=replace_with_your_mysql_password
```

注意：

- `.env` 已加入 `.gitignore`，不要提交。
- API Key 不要写进 README、代码、截图或演示视频。
- 如果 API Key 曾公开发送，建议尽快在平台重置。

## 仓库与分支

学校 GitLab 是比赛正式提交仓库：

- 远程名：`origin`
- 分支：`main`

GitHub 作为同步备份仓库：

- 远程名：`github`
- 分支：`master`

常用推送命令：

```bash
git push
git push github
```

## 当前阶段

当前项目处于设计和基础开发准备阶段。

已完成：

- 项目方向确认
- 技术栈确认
- 中文项目设计文档
- 开发路线图
- 周报模板
- GitLab 与 GitHub 远程仓库配置

下一阶段：

- 配置 MySQL
- 验证 `tju-llm` API 调用
- 建立第一批 Java 核心知识题卡

## 本地运行

### 启动后端

```bash
cd apps/server
mvn spring-boot:run
```

健康检查：

```text
http://localhost:8080/api/health
```

数据库检查：

```text
http://localhost:8080/api/health/db
```

### 启动前端

```bash
cd apps/web
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173/
```

Vite 已配置 `/api` 代理到后端 `http://localhost:8080`。

## 文档

项目文档位于 `docs/` 目录：

- `docs/项目设计.md`：项目定位、功能设计、技术架构和比赛规则适配。
- `docs/总体设计架构.md`：系统总体架构、前后端模块、Agent 状态、数据库表和 API 设计。
- `docs/工程架构决策.md`：参考 `reg-pilot` 后确定的 Java 版本、Monorepo 结构和前后端工程规范。
- `docs/Milvus知识库设计.md`：面经和八股资料向量知识库设计。
- `docs/开发路线图.md`：从 2026 年 7 月到提交截止前的开发计划。
- `docs/任务拆解.md`：按优先级拆分的实施任务、验收标准和第一周具体任务。
- `docs/一周冲刺计划.md`：一周内完成可演示 MVP 的紧凑安排。
- `docs/周报模板.md`：每周开发总结模板。

## 安全与合规

开发时必须遵守：

- 不提交 API Key。
- 不提交 `.env` 文件。
- 不直接复制商业题库或培训机构资料。
- 用户输入简历或项目经历时，应提示删除敏感个人信息。
- 如果实现语音输入，不保存原始音频。

## 项目状态

项目正在开发中，当前重点是先完成最小可用闭环：

> 选择训练模式 -> AI 提问 -> 用户回答 -> AI 追问 -> 生成复盘报告
