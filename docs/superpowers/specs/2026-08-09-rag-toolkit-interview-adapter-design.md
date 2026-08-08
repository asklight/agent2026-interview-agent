# 面试项目接入 rag-toolkit 设计

## 目标

为项目深挖面试增加可选的知识召回能力，同时保持现有面试流程在向量服务不可用时可以继续运行。面试服务只消费召回片段，不向候选人展示相似度分数、命中点或向量库内部元数据。

## 架构

- `VectorRetrievalService` 继续作为面试领域的唯一依赖。
- `NoOpVectorRetrievalService` 在 `rag-toolkit.enabled=false` 或未配置时生效，返回健康的空上下文。
- `RagToolkitVectorRetrievalService` 在开关为 `true` 时生效，通过 `RestClient` 调用 `POST /search`。
- `RagToolkitProperties` 集中管理地址、profile 标签、token、超时和 top-k。

Spring 条件 Bean 保证两种实现不会同时注册。默认关闭，避免本地开发和已有部署环境被动依赖外部服务。

## 数据流

1. 面试应用将当前追问目标和已持久化回答拼成查询文本。
2. 适配器发送 `{query, top_k, filters}`，可选携带 `X-Rag-Token`。
3. 适配器仅提取响应中 `hits[].content` 的非空文本，构造 `RetrievalContext`。
4. 评估器将片段作为上下文使用；召回失败时上下文为空且 `degraded=true`，主流程仍按原有降级策略执行。

## 异常与边界

- 连接超时、读取超时、网络错误、非 2xx 响应和非法 JSON 均记录受控日志并返回降级上下文。
- token 只放在请求头，不写日志、不进入面试响应。
- 空命中列表表示服务正常但没有相关内容，`degraded=false`。
- 适配器不负责写入、更新或删除知识库，也不参与评分计算。

## 验证

单元测试覆盖成功响应、请求体和 token、关闭开关、超时、非 2xx、非法响应及健康空结果。Milvus 和 rag-toolkit 本身继续由独立工具包测试负责。
