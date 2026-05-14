# Wang-Detective

Wang-Detective 是基于 King-Detective 增强的 Oracle Cloud Infrastructure (OCI) 管理面板和 Telegram Bot。它保留原有的 OCI 配置管理、开机任务、实例管理、IP/IPv6、VNC/救援、引导卷、安全规则、Cloudflare DNS、流量统计、备份恢复、MFA 和基础 AI 助手，并补上部署稳定性、诊断能力和运维入口。

## 快速部署

```bash
bash <(wget -qO- https://raw.githubusercontent.com/tony-wang1990/Wang-Detective/main/scripts/install.sh)
```

部署完成后访问：

- Web 面板：`http://your-server-ip:9527`
- 新版功能中心：`http://your-server-ip:9527/wang-features.html`
- 运维终端：`http://your-server-ip:9527/ops-terminal.html`
- 健康检查：`http://your-server-ip:9527/actuator/health`
- 系统诊断：`GET /api/v1/system/diagnostics`，需要登录 token

版本显示说明：新版 Docker 镜像会把构建提交号写入运行版本，例如 `main-b2a3717`；页面上的“最新版本”会查询 `Wang-Detective/main` 最新提交。这样每次修复 BUG 并重建镜像后，都能看到可追踪的版本号，不再显示旧项目 release 的 `null`。

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

## 当前阶段状态

本阶段已经完成第一轮稳定性修复、部署修复、运维入口一期、主面板新功能入口和 UI 重设计的第一阶段落地。当前仓库 `main` 是阶段性收尾版本，后续开发会继续以 UI 重建和前端原生化为一条主线推进。

已完成重点：

- Docker 部署链路已切到 `ghcr.io/tony-wang1990/wang-detective:main`，修复旧 compose、旧容器残留、低配 VPS 启动慢、IPv6 监听和版本 `null` 等问题。
- 后端增强已落地：健康检查、系统诊断、数据库迁移修复、SQLite 分页、WebSocket 日志、VCN/引导卷分页 total、任务前缀隔离。
- 运维入口一期已落地：Web SSH、SSH 主机库、批量命令、SFTP 基础文件操作、操作审计基础表和接口。
- UI 可见性已修复：左侧菜单增加“新版功能”和“运维终端”，并进入主面板 Vue 路由，不再默认跳到独立页面。
- UI 重设计第一阶段已落地：新增现代控制台主题、重做登录页视觉、改造侧边栏/顶部栏/首页卡片，并为首页增加顶部搜索/健康/版本区、地图+系统诊断双栏、资源使用面板。
- Vue 原生化已开始接管生产入口：`frontend/` 为可维护源码，登录页、主框架、首页、配置/任务/日志/系统配置/AI/新版功能/运维终端已进入 Vue 路由。

当前仍未完成、后续必须重点推进：

- **UI 重建和完善仍是下一阶段最大重点。** 生产入口已切到新 Vue，但多数页面还是第一版原生化，需要继续做视觉细节、真实数据、操作状态、异常提示和移动端适配。
- 配置列表、任务列表、服务日志、系统配置、AI 聊天室、运维终端还需要继续按新版 UI 深化，补齐旧版所有细节能力。
- 首页地图目前是新版框架占位，后续要迁入 Leaflet 实时资源地图、指标图表和任务状态。
- 运维终端已具备 Vue 内交互式 Web SSH，后续还要补 SFTP 上传/下载/重命名/删除的原生按钮和审计筛选。
- 旧版完整控制台和旧 bundle 暂时保留为回退入口，验证稳定后再清理。
- 移动端/窄屏布局还需要专项优化。
- Telegram Bot 菜单仍有扩展空间，后续建议增加系统诊断摘要、最近审计、任务状态、主机列表和安全确认后的快捷运维入口。

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

如果启动时报 `KeyError: 'ContainerConfig'`，这是旧版 `docker-compose 1.29.x` 重建新版 GHCR/BuildKit 镜像时的兼容问题。新版安装脚本会优先使用 Docker Compose v2，并在启动前移除旧容器后重新创建；数据目录通过 bind mount 持久化，不会因为删除容器而丢失。

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

1. UI 重建和完善：恢复/重建 Vue 前端源码，把“新版功能”“运维终端”做成真正 Vue 路由；全量重绘登录页、首页、配置列表、任务列表、日志、系统配置、AI 聊天室和运维页面。
2. 运维入口二期：操作审计筛选/导出、权限控制、命令模板、端口转发、大文件传输进度和断点续传。
3. Telegram Bot 增强：系统诊断摘要、最近审计、任务状态、主机列表、快捷命令菜单和危险操作二次确认。
4. OCI Object Storage：Bucket/Object 管理、数据库备份归档、日志归档、临时下载链接。
5. OCI Email Delivery：DKIM/SPF 指引、SMTP 凭据检查、测试发信。
6. 成本、配额、Always Free 用量和超额风险看板。
7. 多云只读资产发现，优先支持 AWS/GCP/Azure/DO 的实例同步。

详细改造记录见 [docs/ENHANCEMENT_REPORT.md](docs/ENHANCEMENT_REPORT.md)。

当前进度、UI 集成约定和后续计划见 [docs/PROJECT_STATUS_AND_UI_GUIDE.md](docs/PROJECT_STATUS_AND_UI_GUIDE.md)。

UI 重设计路线和后续拆分见 [docs/UI_REDESIGN_ROADMAP.md](docs/UI_REDESIGN_ROADMAP.md)。

## 2026-05-14 任务列表原生化增强

本次继续推进 Vue 原生控制台，完成“任务列表”的第二轮增强：

- 任务列表接入真实 `/oci/createTaskPage` 分页参数，支持关键字搜索、架构筛选、页码和每页数量切换。
- 新增任务多选、批量停止、单任务停止和任务详情 JSON 预览。
- 新增 ARM/AMD 架构标识、尝试次数、运行中状态和规格聚合展示，页面风格跟随当前明暗主题。
- 修复后端开机任务分页参数兜底：页码小于 1 或 pageSize 为空时不再产生异常偏移，并限制单页最大 100 条。
- 修复任务 SQL 搜索条件优先级，避免架构筛选与关键字 OR 条件互相串扰。

下一步继续按同一节奏做“服务日志”页面：补齐日志筛选、刷新控制、WebSocket 状态提示、历史日志加载和暗色模式细节。

## 2026-05-13 UI 原生化阶段更新

本次继续推进 UI 重建主线，但保持线上可部署版本稳定：

- 修复 `scripts/install.sh` 被 Windows CRLF 换行污染的问题，避免 Linux 上执行 raw 脚本时报 `$'\r': command not found` 和 `syntax error near unexpected token elif`。
- 扩展 `.gitattributes`，强制 `.sh`、前端源码、HTML/CSS/JS/JSON 等文本文件使用 LF，降低后续再次混入 CRLF 的概率。
- 修复新增模块暗色模式：`新版功能`、`运维终端`、首页新增诊断卡片和内嵌 iframe 会跟随主系统开关灯变化，不再出现暗色外壳里一片白色的页面。
- 新增 `frontend/` 可维护 Vue 源码目录，已落地登录页、控制台主框架、首页、基础路由、主题切换和 API 封装。
- 新 Vue 源码当前构建到 `src/main/resources/dist-next`，暂不替换生产入口 `src/main/resources/dist`。后续等页面迁移完整后，再把 Maven/Docker 构建切到新前端产物。

验证记录：

```bash
npm --prefix frontend install
npm --prefix frontend run build
```

后续第一优先级仍然是 UI 重建和 Vue 原生化：把 `新版功能`、`运维终端` 从静态 HTML/iframe 迁成真正 Vue 路由，再逐步重做配置列表、任务列表、服务日志、系统配置和 AI 聊天室页面。

## 2026-05-14 UI 原生化收口

本次把新 Vue 前端切为正式生产入口，完成今天这一整块 UI 原生化迁移：

- `frontend/` 已成为可维护前端源码，`npm --prefix frontend run build` 直接输出到 `src/main/resources/dist`。
- 登录页、主控制台框架、首页、配置列表、任务列表、服务日志、系统配置、AI 聊天室、新版功能、运维终端都已接入 Vue 原生路由。
- `/dashboard/features` 和 `/dashboard/ops-terminal` 不再依赖 iframe 作为主入口。
- 顶部系统健康状态已改为读取 `/actuator/health`，版本号同步使用健康检查返回值，避免再显示硬编码状态。
- 首页已接入真实 Leaflet 地图和 ECharts 资源图表：使用 `/api/sys/glance` 的城市数据渲染地图点位，并通过 `/metrics/{token}` 实时刷新 CPU、内存和网络流量。
- 配置列表已完成第二轮 Vue 原生化：补齐搜索、开机任务筛选、分页、测活、批量删除、改名、停止开机任务、安全列表放行和详情预览。
- 运维终端已补上 Vue 内交互式 Web SSH：创建会话后直接连接 WebSocket，支持命令输入、发送、Ctrl+C 和断开。
- 运维终端 SFTP 已补齐 Vue 内文件操作：目录浏览、文本读取/编辑保存、上传、下载、新建目录、删除和重命名。
- 登录页左侧品牌区已增强为更明显的 W-探长标识和 OCI 运维控制台信息，不再只是左上角小字。
- 前端接口错误处理已增强，后端返回 `msg/message` 或 `success:false` 时会在页面显示更明确的错误。
- 旧版完整控制台临时保留为 `/legacy-dashboard.html`，便于明天部署测试时对照和回退查看旧功能细节。
- 旧的打包资源暂时保留，等新页面路由细节全部验证通过后再删除旧 bundle 和过渡脚本。

本阶段只做最小验证，已通过：

```bash
npm --prefix frontend run build
```

明天优先测试：登录跳转、暗色/亮色切换、配置列表分页、任务列表、日志 WebSocket、系统配置保存、AI 流式响应、运维终端 SSH/SFTP 真实连接。
