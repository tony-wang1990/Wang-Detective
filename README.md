# King-Detective Enhanced

King-Detective 是一个面向 Oracle Cloud Infrastructure (OCI) 的管理面板和 Telegram Bot，支持 OCI 配置管理、开机任务、实例管理、IP/IPv6、VNC/救援、引导卷、安全规则、Cloudflare DNS、流量统计、备份恢复、MFA 与基础 AI 助手。

本增强版重点修复部署、迁移、WebSocket、健康检查和安全预检问题，并新增系统诊断接口，方便在 VPS 上快速定位常见故障。

## 快速部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/<your-name>/King-Detective-Enhanced/main/scripts/install.sh)
```

部署完成后访问：

- Web 面板: `http://your-server-ip:9527`
- 健康检查: `http://your-server-ip:9527/actuator/health`
- 系统诊断: `GET /api/v1/system/diagnostics`，需要登录 token

默认账号密码仍兼容旧版本：`admin / admin123456`。生产环境请立即在 `/app/king-detective/.env` 中修改：

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

- 数据库: `/app/king-detective/data/king-detective.db`
- OCI 私钥: `/app/king-detective/keys/`
- 应用日志: `/app/king-detective/logs/king-detective.log`
- 更新触发器: `/app/king-detective/runtime/update_version_trigger.flag`
- 环境变量: `/app/king-detective/.env`

## 本版增强

- 修复 Docker Compose 挂载整个工作目录导致镜像内 JAR 被隐藏的问题。
- 修复一键更新触发器与 watcher 判断逻辑不一致的问题。
- 修复 watcher 更新版本号时数据库路径和 `oci_kv.id` 缺失问题。
- 修复数据库迁移脚本被注释行跳过、以及不存在的 `oci_create_task.status` 索引问题。
- 启用 MyBatis Plus SQLite 分页拦截器。
- 修复更换 IP 任务前缀与开机任务前缀冲突的问题。
- 修复 VCN、引导卷分页 total 返回当前页数量的问题。
- 强化 WebSocket token 校验、非法连接关闭、历史日志推送和日志文件初始化。
- 增强 `/actuator/health`，返回版本、运行时长和 JVM 内存信息。
- 新增 `/api/v1/system/diagnostics`，检查数据库、数据目录、密钥目录、日志、默认密码、Bot Token、OpenAI Key、磁盘和内存。
- 清理 POM 重复编译插件，修复项目描述乱码。

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

## 后续路线

对比同类 R-Bot/OCI 管理工具后，后续最值得继续补齐的能力是：

- Web SSH/SFTP、批量命令与端口转发。
- OCI Object Storage Bucket/Object 管理，用于备份、文件投递和日志归档。
- OCI Email Delivery 自动化配置，包括 DKIM、SPF、SMTP 凭据与测试发信。
- 成本、配额、Always Free 用量和超额风险看板。
- 多云只读资产发现，优先支持 AWS/GCP/Azure/DO 的实例同步。

详细改造记录见 [docs/ENHANCEMENT_REPORT.md](docs/ENHANCEMENT_REPORT.md)。
