# King-Detective Enhanced 功能清单

## 已有核心能力

- OCI API 配置管理、上传、分页和详情查看。
- 自动开机任务、批量开机任务、停止任务和失败计数。
- 实例启动、停止、重启、终止、改名、改 Shape、改 CPU/内存/引导卷。
- 公网 IP 更换、IPv6 附加、500Mbps 网络负载均衡辅助。
- VNC/串行控制台、自动救援流程。
- VCN、安全列表入站/出站规则管理。
- 引导卷列表、扩容、VPU 调整和删除。
- Cloudflare DNS 配置与记录增删改查。
- IP 数据、地图视图和流量统计。
- Telegram Bot 操作、通知、备份恢复、MFA、SSH 快捷命令。
- Web 面板、实时日志 WebSocket、系统指标 WebSocket。
- Google 一键登录、MFA、登录失败黑名单、防御模式。

## Enhanced 新增/修复能力

- 部署预检: `GET /api/v1/system/diagnostics`。
- 增强健康检查: `/actuator/health` 返回版本、运行时长、数据库和 JVM 内存状态。
- Docker 数据持久化目录统一为 `data/`、`keys/`、`logs/`。
- watcher 自动更新兼容任意触发内容，并修复数据库版本号写入。
- 数据库迁移脚本真正执行注释后的 SQL，并移除无效索引。
- WebSocket 使用 Spring Bean 注入，修复 token 校验、非法连接关闭和历史日志推送。
- MyBatis Plus SQLite 分页插件启用。
- VCN/引导卷分页 total 修正为筛选后的总记录数。
- 开机任务和更换 IP 任务的内存键前缀隔离，避免互相停止或覆盖。
- CORS 来源可通过 `CORS_ALLOWED_ORIGINS` 配置。
- Maven 构建配置去重，项目描述乱码修复。

## 建议下一阶段

- Web SSH/SFTP/端口转发。
- OCI Object Storage 管理。
- OCI Email Delivery 自动化。
- 成本、配额与 Always Free 风险看板。
- 多云资产发现和只读同步。
