# Wang-Detective

Wang-Detective 是面向 Oracle Cloud Infrastructure (OCI) 的 Web 管理面板和 Telegram Bot 运维助手。当前版本已经从原始 King-Detective 升级为“OCI 管理 + Web 运维 + Telegram Bot + 风险诊断 + 备份恢复 + 救援中心”的增强版控制台。

状态更新时间：2026-05-19

## 快速部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/Wang-Detective/main/scripts/install.sh)
```

部署完成后访问：

- Web 面板：`http://your-server-ip:9527`
- 健康检查：`http://your-server-ip:9527/actuator/health`
- 服务器体检：

```bash
cd /app/king-detective
bash scripts/server-smoke-test.sh
```

生产环境请修改 `/app/king-detective/.env`：

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me_to_strong_password
TELEGRAM_BOT_TOKEN=your_bot_token
TELEGRAM_BOT_CHAT_ID=your_chat_id
```

修改后重启：

```bash
cd /app/king-detective
docker compose up -d --force-recreate
```

## 当前完成度

| 模块 | 完成度 | 当前状态 |
|---|---:|---|
| 部署与更新 | 90% | 安装脚本、Compose v2、watcher、自检脚本、低配 JVM、脚本同步和一键更新链路已完成 |
| Vue 新前端 | 82% | 登录、主框架、首页、配置、任务、日志、系统配置、功能中心、终端、审计、风险、备份、救援中心已原生化 |
| OCI 核心管理 | 82% | 配置、任务、实例详情、实例动作、网络/安全/引导卷等入口已接入真实后端，仍需真机逐项验收 |
| Web SSH/SFTP | 82% | 主机库、Web SSH、命令模板、会话列表、断线重连、resize、SFTP 基础文件操作已完成 |
| Telegram Bot | 78% | 运维中心、诊断、任务、日志、风险、备份、版本更新和实例操作向导已接入 |
| 备份恢复 | 78% | Web 备份统一改为 `backup.sh` 的 tar.gz 格式，恢复计划、定时备份和 Object Storage 归档策略已接入 |
| 救援中心 | 65% | 轻量自救、boot volume 拆卷救援、netboot.xyz 实验区已上线为安全向导，自动救砖仍在实验阶段 |
| CI/测试 | 70% | GitHub Actions 已增加 Java 21、Node 20、前端构建、Maven 构建和前后端接口映射检查 |

## 已完成

- Docker 镜像统一为 `ghcr.io/tony-wang1990/wang-detective:main`。
- 持久化目录统一为 `data/`、`keys/`、`logs/`、`runtime/`、`backups/`。
- 修复部署挂载覆盖 JAR、数据库迁移、SQLite 分页、WebSocket 日志、VCN/引导卷分页 total、任务前缀隔离等基础问题。
- 新增增强健康检查 `/actuator/health` 和系统诊断 `/api/v1/system/diagnostics`。
- 新增可维护 Vue 前端源码 `frontend/`，生产入口已切到新版 Vue。
- 全站移除原生 `alert/confirm/prompt`，改为页面内弹窗、toast、loading 和错误提示。
- 配置列表接入真实 OCI 操作：启动、停止、重启、改名、换 IP、IPv6、VNC、500M、Shape、CPU/内存、引导卷、终止实例等。
- 新增 Web SSH、SSH 主机库、单命令、批量命令、命令模板、会话列表、断线重连、终端 resize。
- 新增 SFTP 浏览、读取、写入、上传、下载、重命名和删除确认。
- 新增操作审计页，支持搜索、筛选、详情和 CSV 导出。
- 新增 OCI 风险看板 `/api/v1/oci/risk`。
- 新增备份归档页 `/api/v1/backups/*`，支持本地备份、Object Storage 归档、恢复计划、定时备份方案。
- 修复 Web 备份格式，统一使用 `scripts/backup.sh` 生成可被 `scripts/restore.sh` 恢复的 `.tar.gz` 备份包。
- 新增救援中心 `/dashboard/rescue` 和 `/api/v1/rescue/*`，提供轻量自救、boot volume 拆卷救援、netboot.xyz 实验区。
- Telegram Bot 运维中心已支持系统诊断、任务状态、最近日志、错误日志、审计摘要、主机概览、风险看板、备份归档、版本更新和实例操作向导。
- Telegram Bot 实例管理新增启动、停止、重启确认执行，并写入操作审计。
- CI 增强：发布前执行脚本语法检查、前端 API 到后端 Controller 映射检查、前端 build、Maven package、Docker build。

## 未完成和后续重点

| 优先级 | 事项 | 说明 |
|---|---|---|
| P0 | 真实 OCI 全量验收 | 需要在真实账号逐个点配置、任务、实例、网络、安全规则、引导卷、备份相关按钮，确认 OCI SDK 返回和错误提示都正确 |
| P0 | 低配 VPS 启动体验 | 当前低配机器首次启动约 60-90 秒属正常范围，但登录页等待和启动提示还可以继续优化 |
| P1 | UI/移动端深度打磨 | 继续检查按钮卡字、窄屏、暗色模式、空状态、失败态和点击反馈 |
| P1 | 备份恢复执行按钮 | 现在 Web 已生成恢复命令和策略；直接从 Web 执行恢复/回滚仍需更严格的二次确认和权限边界 |
| P1 | netboot.xyz 自动救砖 | 当前只做安全向导和脚本，不自动改 bootloader；后续需在测试机实测 AMD/ARM、UEFI/BIOS 后再开放一键引导 |
| P2 | TGBOT 权限模型 | 高危操作后续可增加白名单、管理员确认、操作冷却时间 |
| P2 | API 契约测试 | 继续补 Controller 层 Mock 测试和 Bot 回调测试，减少上线后才发现接口问题 |

## 常用命令

```bash
cd /app/king-detective

# 查看服务和日志
docker compose ps
docker logs -f king-detective

# 手动更新、回滚、体检
bash scripts/update.sh
bash scripts/rollback.sh ghcr.io/tony-wang1990/wang-detective:main
bash scripts/server-smoke-test.sh

# 备份、恢复、支持包
bash scripts/backup.sh
bash scripts/restore.sh /app/king-detective/backups/wang-detective-backup-YYYYmmdd-HHMMSS.tar.gz
bash scripts/support-bundle.sh
```

## 重要文档

- 部署验收：[docs/DEPLOYMENT_SMOKE_TEST.md](docs/DEPLOYMENT_SMOKE_TEST.md)
- 代码审计：[docs/CODE_AUDIT_REPORT.md](docs/CODE_AUDIT_REPORT.md)
- 项目路线：[docs/PROJECT_PROGRESS_ROADMAP.md](docs/PROJECT_PROGRESS_ROADMAP.md)
- UI 路线：[docs/UI_REDESIGN_ROADMAP.md](docs/UI_REDESIGN_ROADMAP.md)
- 救援/netboot 路线：[docs/RESCUE_NETBOOT_ROADMAP.md](docs/RESCUE_NETBOOT_ROADMAP.md)
