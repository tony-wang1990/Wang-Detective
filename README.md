# Wang-Detective

Wang-Detective 是基于 King-Detective 增强的 Oracle Cloud Infrastructure (OCI) 管理面板和 Telegram Bot。它保留原有的 OCI 配置管理、开机任务、实例管理、IP/IPv6、VNC/救援、引导卷、安全规则、Cloudflare DNS、流量统计、备份恢复、MFA 和基础 AI 助手，并补上部署稳定性、诊断能力和运维入口。

## 快速部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/Wang-Detective/main/scripts/install.sh)
```

部署完成后访问：

- Web 面板：`http://your-server-ip:9527`
- 运维终端：`http://your-server-ip:9527/ops-terminal.html`
- 健康检查：`http://your-server-ip:9527/actuator/health`
- 系统诊断：`GET /api/v1/system/diagnostics`，需要登录 token

默认账号密码兼容旧版本：`admin / admin123456`。生产环境请立即在 `/app/king-detective/.env` 中修改：

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change_me_to_strong_password
TELEGRAM_BOT_TOKEN=your_bot_token
```

然后重启：

```bash
cd /app/king-detective
docker compose up -d --force-recreate
```

## 数据目录

增强版统一使用可持久化目录，避免更新镜像时丢数据：

- 数据库：`/app/king-detective/data/king-detective.db`
- OCI 私钥：`/app/king-detective/keys/`
- 应用日志：`/app/king-detective/logs/king-detective.log`
- 更新触发器：`/app/king-detective/runtime/update_version_trigger.flag`
- 环境变量：`/app/king-detective/.env`

## 本版增强

- 修复 Docker Compose 挂载整个工作目录导致镜像内 JAR 被隐藏的问题。
- 统一 `data/`、`keys/`、`logs/`、`runtime/` 持久化目录。
- 修复 watcher 更新触发、数据库版本写入和 `oci_kv.id` 缺失问题。
- 修复数据库迁移 SQL 被注释跳过和无效索引问题。
- 启用 MyBatis Plus SQLite 分页插件。
- 修复 VCN、引导卷分页 total 只返回当前页数量的问题。
- 隔离开机任务和换 IP 任务的内存键前缀，避免互相覆盖。
- 强化日志 WebSocket token 校验、非法连接关闭、历史日志推送和日志文件初始化。
- 增强 `/actuator/health`，返回版本、运行时长、数据库和 JVM 内存状态。
- 新增 `/api/v1/system/diagnostics`，检查数据库、数据目录、密钥目录、日志、默认密码、Bot Token、OpenAI Key、磁盘和内存。
- 新增一期运维入口：Web SSH 终端、SSH 单命令、批量命令、SFTP 列表/读取/写入/上传/下载/重命名/删除。
- 新增 SSH 主机资产库：保存常用主机、AES-GCM 加密保存密码/私钥、通过 `hostId` 复用凭据。

## 运维终端

登录 Web 面板后，可以直接打开：

```text
http://your-server-ip:9527/ops-terminal.html
```

页面会自动读取浏览器 `sessionStorage` 中的登录 token，也可以手动粘贴 token。当前一期能力包括：

- 测试 SSH 连接。
- 保存/更新常用 SSH 主机，后续直接选择主机执行操作。
- 打开交互式 Web SSH 终端。
- 执行单台机器命令并返回 stdout/stderr/exit status。
- 批量对多台机器执行同一条命令。
- SFTP 浏览目录、读取小文本文件、写入文件、上传/下载文件、创建目录、重命名和删除。

安全提醒：保存的 SSH 密码/私钥会使用 AES-GCM 加密后写入数据库。建议生产环境显式配置稳定的 `OPS_SSH_SECRET_KEY`，否则默认会从 Web 管理密码派生密钥；如果后续修改管理密码，旧的保存主机可能无法解密。Web SSH 会话凭据仍仅短时保存在服务端内存中。后续会继续补充操作审计、权限控制、端口转发和大文件传输进度。

## 常用命令

```bash
cd /app/king-detective

# 查看状态
docker compose ps

# 查看日志
docker logs -f king-detective

# 重启
docker compose restart king-detective

# 手动更新
TELEGRAM_BOT_TOKEN="xxx" ADMIN_USERNAME="admin" ADMIN_PASSWORD="strong_password" bash update.sh
```

## 部署问题处理

如果安装时卡在 `Pulling websockify ... error`，说明服务器上保留了早期增强版的旧 `docker-compose.yml`，其中引用了未发布的 `king-detective-websockify` 镜像。新版默认部署已移除该非必需服务，并会自动备份旧 compose 后刷新。

如果 1C/1G 左右的 VPS 部署后长时间 `health: starting` 或 `unhealthy`，通常是首次 Spring 初始化太慢，不一定是程序崩溃。新版默认限制 JVM 使用 1 个 CPU、384MB 堆内存、IPv4 监听，并把健康检查启动宽限延长到 10 分钟；`watcher` 也改为可选 profile，避免低配机器默认多跑一个容器。

可在服务器上执行：

```bash
cd /app/king-detective
cp docker-compose.yml docker-compose.yml.bak.$(date +%Y%m%d%H%M%S) 2>/dev/null || true
rm -f docker-compose.yml
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/Wang-Detective/main/scripts/install.sh)
```

需要启用自动更新 watcher 时再执行：

```bash
cd /app/king-detective
docker compose --profile watcher up -d
```

## 本地验证

本项目按 Java 21 构建：

```bash
mvn -DskipTests compile
mvn test
mvn package
```

## 后续路线

对比 R-Bot / java_oci_manage 等同类 OCI 运维工具后，下一阶段建议按这个顺序继续：

1. 运维入口二期：操作审计、权限控制、端口转发、大文件传输进度和断点续传。
2. OCI Object Storage：Bucket/Object 管理、数据库备份归档、日志归档、临时下载链接。
3. OCI Email Delivery：DKIM/SPF 指引、SMTP 凭据检查、测试发信。
4. 成本、配额、Always Free 用量和超额风险看板。
5. 多云只读资产发现，优先支持 AWS/GCP/Azure/DO 的实例同步。

详细改造记录见 [docs/ENHANCEMENT_REPORT.md](docs/ENHANCEMENT_REPORT.md)。
