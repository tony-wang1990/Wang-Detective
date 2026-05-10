# King-Detective Enhanced 改造报告

## 调研结论

对比 R-Bot / `semicons/java_oci_manage` 一类项目后，OCI 管理工具的高价值能力集中在四类：

- 运维入口：Web SSH、SFTP、批量命令、端口转发、主机同步。
- 云资源闭环：实例、网络、卷、DNS、对象存储、Email Delivery。
- 风险看板：Always Free 配额、成本、流量、启动失败原因和容量不足重试。
- 安全边界：私钥本地保存、登录保护、MFA、审计、更新可回滚。

本次没有直接复制同类项目代码，只吸收产品功能方向；优先处理当前仓库里会影响部署和稳定性的短板，并补上系统诊断能力。

## 主要代码改造

### 部署与更新

- `docker-compose.yml` 不再把整个当前目录挂载到 `/app/king-detective`，避免覆盖镜像内 JAR。
- 数据库、私钥、日志分别挂载到 `data/`、`keys/`、`logs/`。
- `Dockerfile` 增加健康检查并统一 `KING_DETECTIVE_VERSION`。
- `scripts/install.sh` 生成 `.env`，不再下载仓库中不存在的 `king-detective.db`。
- `scripts/watcher.sh` 接受任意非空触发内容，修复 DB 路径和 `oci_kv.id` 插入。
- `update.sh` 同时兼容 `TELEGRAM_BOT_TOKEN` 与旧的 `BOT_TOKEN`。

### 数据库

- `DatabaseMigrationRunner` 会先移除 SQL 注释行再拆分执行，避免迁移语句被整段跳过。
- `migration_v4_0.sql` 移除不存在字段 `oci_create_task.status` 的索引。
- 启用 MyBatis Plus SQLite 分页拦截器。

### 运行稳定性

- 修复 `CHANGE_IP_TASK_PREFIX` 与 `CREATE_TASK_PREFIX` 相同的问题。
- 修复 VCN、引导卷分页 total 只返回当前页数量的问题。
- WebSocket 日志处理改用 Spring 管理的 `LogWebSocketHandler`，支持 `@Value` 注入。
- WebSocket 非法 token 会主动关闭连接，历史日志条件判断修复。
- JWT 过期判断对畸形 token 返回过期，避免异常冒泡成 500。
- OCI API 重试切面开始尊重 `@RetryableOciApi(maxAttempts, delayMs)` 参数。
- API 限流异常使用标准 HTTP 429。

### 安全与可观测

- CORS 来源支持 `CORS_ALLOWED_ORIGINS` 配置。
- `/actuator/health` 增加版本、运行时长、JVM 内存。
- 新增 `GET /api/v1/system/diagnostics`，输出数据库、目录、日志、默认密码、Bot Token、OpenAI Key、磁盘和运行时状态。
- README、FEATURES、部署说明更新为增强版说明。

## 建议下一阶段功能

1. Web SSH/SFTP/端口转发：同类项目里使用频率最高，能让面板从“云 API 管理”升级到“实际运维入口”。
2. OCI Object Storage：适合做数据库备份归档、日志归档、文件中转、临时下载链接。
3. OCI Email Delivery：自动生成 DKIM/SPF 指引、SMTP 凭据检查和测试发信。
4. Always Free 风险看板：汇总 A1 OCPU/内存、200GB 块存储、实例数、流量、可能计费项。
5. 配额与容量失败分析：按区域记录 `InsufficientHostCapacity`、`LimitExceeded`，给出重试建议。
6. 审计页面：把已有 `audit_log` 表真正接入查询、导出和筛选。
