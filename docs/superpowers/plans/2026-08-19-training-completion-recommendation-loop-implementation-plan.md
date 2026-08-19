# 训练完成后推荐闭环实施计划

日期：2026-08-19

对应规格：`docs/superpowers/specs/2026-08-19-training-completion-recommendation-loop-design.md`

## 1. 路由契约

- 在 `training-agent/model` 中定义一次性 query 常量和首页目标生成函数。
- 为路由生成和非法 query 处理增加单元测试。

## 2. Dashboard 刷新状态

- 抽取有界刷新函数，支持立即请求、延迟重试、取消和失败保留最后值。
- 默认最多三次请求，不创建并发轮询。
- 增加 fake timer 单元测试。

## 3. 首页接入

- 识别训练完成 query，并显示同步中的自然语言状态。
- 刷新完成后用 `router.replace` 清理一次性 query。
- 请求失败时显示手动重试入口，保持四个模块可用。
- 保持 `COLD_START / READY / DISABLED / DEGRADED` 现有展示语义。

## 4. 报告页入口

- 知识、项目、算法和综合模拟报告增加“查看下一步训练”。
- 所有入口使用同一个路由 helper。
- 入口只在报告已经存在时显示。

## 5. 验证

- 运行训练 Agent、首页和四类报告相关测试。
- 运行前端全量测试与生产构建。
- 检查 `git diff --check`。
- 确认没有提交用户未跟踪的学习文档。
