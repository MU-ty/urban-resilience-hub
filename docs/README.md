# 城市共享站：后端学习文档总目录

这套文档结合当前项目源码编写，适合边运行、边读代码、边做实验。正文、实验、问答和验收分开，方便查阅。

## 从哪里开始

| 你现在的目标 | 先读 |
|---|---|
| 先把项目跑起来 | [项目 README](../README.md) 与 [实验 0](实验手册.md) |
| 系统学 Java 后端 | [后端学习手册](后端学习手册.md)：22 章 |
| 通过实验理解事务、缓存和 MQ | [实验手册](实验手册.md)：16 组实验 |
| 准备项目面试 | [面试问答与进阶练习](面试问答与进阶练习.md)：55 问、10 个源码练习 |
| 确认改了什么、测试了什么 | [改造与验收](改造与验收.md) |

建议节奏：每天一个主题，运行一个实验，写一页自己的故障或原理记录。不要只看文档不动手，也不要只跑命令不解释结果。

## 源码快捷入口

| 学习点 | 源文件 |
|---|---|
| 请求入口与分页 DTO | [ReliefRequestController](../src/main/java/com/example/resilience/web/ReliefRequestController.java) |
| 归属权限、幂等回放、筛选 | [ReliefRequestService](../src/main/java/com/example/resilience/service/ReliefRequestService.java) |
| 事务分配 | [AllocationService](../src/main/java/com/example/resilience/service/AllocationService.java) |
| 请求行锁 | [ReliefRequestRepository](../src/main/java/com/example/resilience/repository/ReliefRequestRepository.java) |
| FEFO 库存锁 | [SupplyLotRepository](../src/main/java/com/example/resilience/repository/SupplyLotRepository.java) |
| Redis 租约 | [IdempotencyService](../src/main/java/com/example/resilience/service/IdempotencyService.java) |
| Redis 缓存 | [ShelterQueryService](../src/main/java/com/example/resilience/service/ShelterQueryService.java) |
| Redis 原子限流 | [RateLimitFilter](../src/main/java/com/example/resilience/security/RateLimitFilter.java) |
| Outbox 发布确认 | [OutboxPublisher](../src/main/java/com/example/resilience/service/OutboxPublisher.java) |
| 安全过滤器链 | [SecurityConfig](../src/main/java/com/example/resilience/config/SecurityConfig.java) |
| 全局错误处理 | [GlobalExceptionHandler](../src/main/java/com/example/resilience/common/GlobalExceptionHandler.java) |
| 初始数据库 | [V1](../src/main/resources/db/migration/V1__init.sql) |
| 本轮数据库升级 | [V2](../src/main/resources/db/migration/V2__request_idempotency_and_query_indexes.sql) |
| 前端行为 | [app.js](../src/main/resources/static/assets/app.js) |
| 真实 API 验证 | [api-smoke.mjs](../tests/api-smoke.mjs) |

## 文档的事实边界

“已经实现”以当前源码和测试为准；“实验步骤”不代表每个故障实验都已执行；“进阶练习”代表尚待扩展的任务。整套资料覆盖常见后端基础主线，但没有声称穷尽全部 Java 生态知识。

阅读过程中若发现本机版本不同，应先核对 pom.xml、application.yml 和实际运行 JDK。官方链接用于核对原理和版本契约，不能替代本项目的实际测试结果。

