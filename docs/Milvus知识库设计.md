# Milvus 知识库设计

## 1. 定位

本项目使用 Milvus 作为面经和八股资料的向量知识库。

Milvus 不替代结构化题卡系统，而是作为增强层：

```text
结构化题卡：保障面试流程、主问题、追问链、评分标准稳定。
Milvus 知识库：从大量八股总结、面经、项目追问案例中召回相关片段。
```

最终目标是形成混合架构：

```text
题卡检索
  + Milvus 语义召回
  + tju-llm 评价/追问/复盘
```

## 2. 为什么使用 Milvus

使用 Milvus 的原因：

- 用户会收集大量八股总结和面经。
- 面经内容是非结构化文本，适合向量检索。
- 可以学习真实向量库、embedding、灌库、召回和 RAG 工程。
- 后续项目经历深挖时，可以检索相似技术栈的常见追问。
- 复盘报告可以基于召回内容推荐补强资料。

不使用 Milvus 做的事情：

- 不用它保存会话状态。
- 不用它替代 MySQL。
- 不用它直接决定评分。
- 不让它成为 P0 面试闭环的唯一依赖。

## 3. 总体架构

```text
apps/web
  -> apps/server
       -> MySQL              # 题卡、会话、回答、报告
       -> Milvus             # 面经/八股资料向量检索
       -> tju-llm API         # 面试提问、评价、追问、复盘

apps/ingest
  -> 清洗八股/面经文本
  -> chunk
  -> embedding
  -> 写入 Milvus
```

说明：

- `apps/server` 是运行时主服务。
- `apps/ingest` 是离线灌库工具，不是线上 agent 服务。
- 线上面试过程仍由 Spring Boot 控制。
- 大模型调用仍走学校 `tju-llm` API。

## 4. 数据来源

计划纳入 Milvus 的资料：

- Java 八股总结
- MySQL 八股总结
- Redis 八股总结
- Spring / Spring Boot 八股总结
- JVM / 并发总结
- 真实面经
- 项目追问案例
- 算法口述模板

每条资料需要先清洗：

- 去掉广告、公众号无关内容、版权风险明显的内容。
- 去掉重复内容。
- 保留来源类型和模块标签。
- 切成适合检索的小片段。

## 5. Collection 设计

Milvus collection 建议名称：

```text
interview_knowledge_chunk
```

字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | VarChar | chunk 唯一 ID |
| `content` | VarChar | 文本片段 |
| `embedding` | FloatVector | 向量 |
| `source_type` | VarChar | 八股总结 / 面经 / 项目追问 / 算法 |
| `module` | VarChar | Java / MySQL / Redis / Spring / JVM 等 |
| `difficulty` | VarChar | easy / medium / hard，可为空 |
| `tags` | VarChar | 标签，逗号分隔 |
| `source_title` | VarChar | 来源标题 |
| `source_url` | VarChar | 来源链接，可为空 |
| `chunk_index` | Int64 | 在原文中的序号 |
| `created_at` | Int64 | 入库时间戳 |

第一版可以先简化为：

- `id`
- `content`
- `embedding`
- `source_type`
- `module`
- `tags`
- `source_title`

## 6. Embedding 模型

学校 API 指南目前主要提供 `chat/completions`，没有明确 embedding 接口。

因此向量入库建议使用本地开源 embedding 模型。

第一版推荐：

```text
BAAI/bge-small-zh-v1.5
```

原因：

- 中文效果够用。
- 模型较轻。
- 适合本地灌库。
- 向量维度 512，Milvus 存储压力较小。

后续可升级：

```text
BAAI/bge-m3
```

但 `bge-m3` 更重，不建议作为第一版灌库模型。

## 7. 检索流程

面试运行时：

```text
当前问题 / 用户回答 / 模块 / 标签
  -> 生成检索 query
  -> embedding query
  -> Milvus topK 检索
  -> metadata 过滤
  -> 拼接召回片段
  -> 交给 tju-llm 评价、追问或复盘
```

示例：

用户正在回答 Redis 缓存一致性。

检索条件：

```text
module = Redis
tags contains cache, consistency
topK = 3
```

召回内容用于辅助：

- 判断用户漏掉哪些点。
- 生成更贴近真实面经的追问。
- 在复盘里推荐补强内容。

## 8. 与题卡系统的关系

题卡系统负责稳定性：

- 主问题
- 标准要点
- 追问链
- 评分标准

Milvus 负责丰富性：

- 面经补充
- 相似问题
- 技术背景
- 复盘建议

评分时的优先级：

```text
题卡评分点 > Milvus 召回内容 > LLM 自身知识
```

这样可以避免向量召回错误导致评分跑偏。

## 9. 开发阶段安排

### P0：不依赖 Milvus 的面试闭环

- Java 核心知识训练
- 学校 API 调用
- 追问
- 基础复盘

### P1：Milvus 灌库

- 建立 Milvus docker 环境
- 建立 `apps/ingest`
- 清洗第一批八股资料
- embedding
- 写入 Milvus
- 实现检索测试

### P2：Milvus 增强面试流程

- 在回答评价时召回相关知识片段
- 在追问生成时参考面经片段
- 在复盘报告中推荐相似资料
- 在项目经历深挖时检索相似技术栈面经

## 10. 风险控制

风险：Milvus 部署和灌库拖慢主流程。

控制：

- Milvus 不进入 P0 必须项。
- P0 面试闭环不依赖 Milvus。
- Milvus 先做独立检索测试，成功后再接入面试流程。

风险：召回内容质量差。

控制：

- 入库前清洗数据。
- 保留 module、tags、source_type 等 metadata。
- 检索时先 metadata 过滤，再向量相似度排序。

风险：embedding 模型和学校 API 规则冲突。

控制：

- embedding 仅用于离线灌库。
- 线上面试的大模型调用仍走学校 `tju-llm` API。
- 不把外部聊天模型作为运行时生成模型。

