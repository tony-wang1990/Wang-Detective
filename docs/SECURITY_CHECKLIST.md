# ✅ 安全修复检查清单

## 🚨 紧急措施 (立即执行)

### ☐ 1. 撤销泄露的Token
- [ ] 打开 [@BotFather](https://t.me/BotFather)
- [ ] 发送 `/mybots`
- [ ] 选择你的Bot
- [ ] 点击 `API Token`
- [ ] 点击 `Revoke current token`
- [ ] **确认撤销**
- [ ] 复制新Token到安全位置

**完成时间**:_______

---

## 🔧 配置更新

### ☐ 2. 创建环境变量文件
- [ ] 创建 `.env` 文件（项目根目录）
- [ ] 添加 `TELEGRAM_BOT_TOKEN=新Token`
- [ ] 添加 `WEB_ACCOUNT=your_account`
- [ ] 添加 `WEB_PASSWORD=strong_password`
- [ ] **确认 `.env` 不会被提交到Git**

**完成时间**:_______

### ☐ 3. 验证配置文件
- [ ] `application.yml` 使用环境变量 ✅ (已完成)
- [ ] `.gitignore` 包含 `.env` ✅ (已完成)
- [ ] `.env.example` 已创建 ✅ (已完成)

---

## 📦 代码提交

### ☐ 4. 提交安全更改
```bash
git add .gitignore .env.example src/main/resources/application.yml
git commit -m "security: use environment variables for sensitive data"
git push
```

- [ ] `.gitignore` 已提交
- [ ] `.env.example` 已提交
- [ ] `application.yml` 已提交
- [ ] **确认 `.env` 未被提交**

**完成时间**:_______

---

## 🚀 部署验证

### ☐ 5. 重启服务
```bash
# Docker
docker-compose down
docker-compose up -d

# 或本地
# 停止原服务，重新启动
```

- [ ] 服务已重启
- [ ] 环境变量已加载
- [ ] 无启动错误

**完成时间**:_______

### ☐ 6. 功能测试
- [ ] 向Bot发送 `/start`
- [ ] Bot正常响应
- [ ] 所有功能正常
- [ ] 日志无异常

**测试结果**: _______ (通过/失败)

---

## 🔍 GitHub处理

### ☐ 7. 关闭安全警报
- [ ] 进入GitHub仓库
- [ ] 点击 **Security** 标签
- [ ] 找到 "Telegram Bot Token #1"
- [ ] 点击 **Close as** → **Revoked**
- [ ] 添加备注: "Token已撤销并轮换"

**完成时间**:_______

### ☐ 8. 清理Git历史 (可选)
**⚠️ 只在必要时执行，会改写历史！**

选择一个方法：
- [ ] 方法1: git-filter-repo
- [ ] 方法2: BFG Repo-Cleaner  
- [ ] 方法3: 删除仓库重建
- [ ] 跳过此步骤

**完成时间**:_______ (或 N/A)

---

## 🛡️ 长期防护

### ☐ 9. 设置防护措施
- [ ] Pre-commit hook已配置
- [ ] `.gitattributes` 已设置
- [ ] 团队成员已通知

### ☐ 10. 文档更新
- [ ] README添加环境变量说明
- [ ] 部署文档已更新
- [ ] 开发人员已培训

---

## 📊 验证报告

### 环境变量检查
```bash
# 验证环境变量已加载
docker exec king-detective env | grep TELEGRAM
```

- [ ] TELEGRAM_BOT_TOKEN 已设置
- [ ] WEB_ACCOUNT 已设置
- [ ] WEB_PASSWORD 已设置

### Git仓库检查
```bash
# 确认敏感文件未被跟踪
git ls-files | grep -E '\.env$|\.secret$'
```

- [ ] 无敏感文件在Git中

### 安全扫描
```bash
# 使用 trufflehog 或 gitleaks 扫描
git secrets --scan
```

- [ ] 无新的密钥泄露

---

## ✅ 最终确认

- [ ] ✅ 旧Token已撤销
- [ ] ✅ 新Token已配置（环境变量）
- [ ] ✅ 代码已更新并提交
- [ ] ✅ 服务重启成功
- [ ] ✅ 功能测试通过
- [ ] ✅ GitHub警报已关闭
- [ ] ✅ 防护措施已设置

**完成日期**: _______  
**负责人**: _______  
**验证人**: _______

---

## 🎯 成功标准

- ✅ Bot正常运行
- ✅ 无安全警报
- ✅ `.env` 未在Git中
- ✅ 所有敏感信息使用环境变量
- ✅ 团队成员已培训

---

## 📞 遇到问题？

### Bot无响应
1. 检查Token是否正确
2. 检查环境变量是否加载
3. 查看服务日志

### 环境变量未加载
1. 检查 `.env` 文件位置
2. 重启Docker服务
3. 验证docker-compose.yml配置

### GitHub警报无法关闭
1. 确认Token已撤销
2. 等待几分钟
3. 联系GitHub支持

---

**安全是持续的过程，不是一次性任务！** 🔒
