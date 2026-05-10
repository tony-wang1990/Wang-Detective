# Wang-Detective 功能清单

## 已有核心能力

- OCI API 配置管理、上传、分页和详情查看。
- 自动开机任务、批量开机任务、停止任务和失败计数。
- 实例启动、停止、重启、终止、改名、改 Shape、改 CPU/内存/引导卷。
- 公网 IP 更换、IPv6 附加、网络负载均衡辅助。
- VNC/串行控制台和自动救援流程。
- VCN、安全列表入站/出站规则管理。
- 引导卷列表、扩容、VPU 调整和删除。
- Cloudflare DNS 配置与记录增删改查。
- IP 数据、地图视图和流量统计。
- Telegram Bot 操作、通知、备份恢复、MFA 和 SSH 快捷命令。
- Web 面板、实时日志 WebSocket、系统指标 WebSocket。
- Google 一键登录、MFA、登录失败黑名单、防御模式。

## Enhanced 修复能力

- 部署预检：`GET /api/v1/system/diagnostics`。
- 增强健康检查：`/actuator/health` 返回版本、运行时长、数据库和 JVM 内存状态。
- Docker 数据持久化目录统一为 `data/`、`keys/`、`logs/`、`runtime/`。
- watcher 自动更新兼容任意触发内容，并修复数据库版本号写入。
- 数据库迁移脚本真正执行注释后的 SQL，并移除无效索引。
- WebSocket 使用 Spring Bean 注入，修复 token 校验、非法连接关闭和历史日志推送。
- MyBatis Plus SQLite 分页插件启用。
- VCN/引导卷分页 total 修正为筛选后的总记录数。
- 开机任务和更换 IP 任务的内存键前缀隔离，避免互相停止或覆盖。
- CORS 来源可通过 `CORS_ALLOWED_ORIGINS` 配置。
- Maven 构建配置去重，项目描述乱码修复。

## 新增一期运维入口

- Web SSH 交互式终端：`/ops-terminal.html`。
- SSH 主机资产库：保存、更新、删除常用主机。
- SSH 凭据加密保存：密码、私钥和私钥口令使用 AES-GCM 加密后写入数据库。
- SSH 连接测试：`POST /api/ops/ssh/test`。
- SSH 单命令执行：`POST /api/ops/ssh/exec`。
- SSH 批量命令执行：`POST /api/ops/ssh/batch`。
- SFTP 目录列表：`POST /api/ops/sftp/list`。
- SFTP 小文本读取：`POST /api/ops/sftp/read`。
- SFTP 文本写入：`POST /api/ops/sftp/write`。
- SFTP 文件下载：`POST /api/ops/sftp/download`。
- SFTP 文件上传：`POST /api/ops/sftp/upload`，当前基于保存主机 `hostId`。
- SFTP 创建目录、删除、重命名：`/api/ops/sftp/mkdir`、`/api/ops/sftp/delete`、`/api/ops/sftp/rename`。
- 运维操作审计：SSH 主机变更、终端会话、命令执行、批量命令和 SFTP 写入/上传/下载/删除会写入 `audit_log`。
- 最近审计记录查询：`GET /api/ops/audit/recent`。

## 建议下一阶段

- 权限控制、操作审计和敏感操作二次确认。
- 端口转发、大文件传输进度和断点续传。
- OCI Object Storage 管理。
- OCI Email Delivery 自动化。
- 成本、配额与 Always Free 风险看板。
- 多云资产发现和只读同步。
