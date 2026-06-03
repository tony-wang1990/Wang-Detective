# Wang-Detective 代码审计与优化报告

审计日期：2026-06-03

## 结论

本轮对 Web 前端、后端 Controller/Service、OCI SDK 调用、脚本、Telegram Bot 回调和部署链路做了收口审计。当前核心功能已经不是纯文字说明或静态假按钮：配置详情、实例操作、风险扫描、备份归档、救援入口、安全规则和 TGBOT 菜单均有后端接口承接；其中高危动作保留二次确认和审计边界。

需要特别说明：netboot.xyz 自动改启动链属于高危实验能力，本版本提供脚本生成和安全入口，不做无确认的静默改写；Boot Volume 拆卷救援和终止实例等动作仍建议只在专用测试资源上验收。

## 本轮已修复

| 范围 | 完成情况 |
|---|---|
| TGBOT 回调 | 点击按钮后立即 `answerCallbackQuery`，减少菜单转圈等待；重复回调答复不再误报“处理请求错误” |
| TGBOT 菜单映射 | `scripts/verify-telegram-callbacks.mjs` 静态扫描 120 个 callback，全部能匹配 handler |
| TGBOT 通知降噪 | 开机任务只保留成功和明确失败等结果通知，过程性任务状态不再刷屏推送到 TG |
| TG 配置读取 | Telegram Token/Chat ID 从数据库读取时按最新记录取值，避免历史重复配置导致 `selectOne` 异常 |
| Web API 映射 | `scripts/verify-ui-api-mapping.mjs` 扫描 72 个前端 API 调用，全部有后端 Controller 映射 |
| 备份恢复 | Web 端新增一键恢复本地备份、一键安装/关闭定时备份，watcher 执行动作，命令仅作为兜底 |
| 救援中心 | 新增一键自动救援表单，选择 OCI 配置和实例后调用 `/api/oci/autoRescue`；脚本区作为兜底和实验说明 |
| 风险看板 | 风险判断扩展到配置、实例、ARM 免费资源、引导卷容量、扫描异常和公网网络暴露，不再只看端口 |
| 安全规则 | 配置详情里的规则明细支持新增/删除入站和出站安全规则，直接调用 OCI 安全列表接口 |
| 登录配置 | 系统配置新增修改当前 Web 登录账号和密码，保存到 SQLite `oci_kv`，登录和 WebSocket token 校验同步使用新凭据 |

## 功能真实性

| 功能区 | 是否真实调用 | 说明 |
|---|---|---|
| 配置列表 | 是 | 列表来自本地 SQLite；实时资源按钮调用 `/api/oci/details` 读取 OCI SDK 返回 |
| 实例动作 | 是 | 启动、停止、重启、改名、Shape、CPU/内存、引导卷、换 IP、IPv6、VNC、终止等均走后端 OCI Service |
| 安全规则明细 | 是 | 入站/出站列表、添加、删除均调用 `/api/securityRule/*`，会修改 OCI 默认 Security List |
| 风险看板 | 是 | 读取实例、VCN、安全规则、引导卷等 OCI 实时/短期缓存数据后生成风险 |
| 备份归档 | 是 | 本地备份执行 `scripts/backup.sh`，Object Storage 上传/删除走 OCI Object Storage SDK |
| 一键恢复/定时备份 | 是 | Web 提交动作文件到 `runtime/`，watcher 调用 `restore.sh` 或内部定时备份调度 |
| 救援中心 | 部分自动 | 一键自动救援调用 `/api/oci/autoRescue`；netboot.xyz 保持实验脚本入口，避免误刷启动链 |
| 运维终端 | 是 | Web SSH/SFTP 通过后端 SSH 服务连接目标主机 |
| TGBOT | 是 | 诊断、任务、日志、审计、风险、备份、实例菜单均由后端服务或 OCI SDK 返回；按钮映射已静态验收 |

## 仍需人工验收的边界

| 项目 | 原因 |
|---|---|
| 终止实例、拆卷救援、恢复回滚 | 高危破坏性操作，不能在生产资源上自动全量验证 |
| netboot.xyz 一键救砖 | 涉及 bootloader、UEFI/BIOS、ARM/AMD 差异，需要专用测试实例确认 |
| Object Storage 云端恢复 | 当前闭环是先下载到本地 `backups/` 再一键恢复，后续可补“从对象直接拉取并恢复” |
| TGBOT 可控执行权限 | 已补回调与菜单稳定性，后续可继续加管理员二次确认、冷却时间和角色权限 |

## 验证记录

```bash
node scripts/verify-telegram-callbacks.mjs
node scripts/verify-ui-api-mapping.mjs
npm --prefix frontend run build
```

当前静态映射检查已通过；前端构建和后端 Maven 编译结果以本次提交后的本地/CI输出为准。部署后建议继续执行：

```bash
bash scripts/remote-smoke-test.sh https://oci.199060.xyz admin '***'
```
