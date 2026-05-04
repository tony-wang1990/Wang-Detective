# King-Detective 一键更新脚本使用说明

## 安全说明

**⚠️ 重要**: 本脚本不包含任何敏感信息（如Token、密码），所有配置需要通过环境变量传入，确保安全。

## 使用方法

### 方法1: 设置环境变量后运行

```bash
# 1. 设置环境变量（替换为你的实际值）
export BOT_TOKEN="你的Telegram_Bot_Token"
export ADMIN_USERNAME="你的管理员用户名"
export ADMIN_PASSWORD="你的管理员密码"

# 2. 执行更新脚本
bash update.sh
```

### 方法2: 一行命令运行

```bash
BOT_TOKEN="你的Token" ADMIN_USERNAME="你的用户名" ADMIN_PASSWORD="你的密码" bash update.sh
```

## 脚本功能

✅ **保留所有数据**
- 数据库文件（账户、配置、任务记录）
- 私钥文件
- 所有历史数据

✅ **自动备份**
- 更新前自动备份数据库到 `king-detective.db.backup.时间戳`

✅ **安全更新流程**
1. 检查并创建数据目录
2. 备份现有数据库
3. 停止旧容器
4. 拉取最新镜像
5. 启动新容器（挂载原有数据卷）
6. 验证服务状态

## 数据位置

- **数据库**: `/root/king-detective/data/king-detective.db`
- **私钥**: `/root/king-detective/keys/`
- **备份**: `/root/king-detective/data/king-detective.db.backup.*`

## 常见问题

### Q: 更新后数据会丢失吗？
A: 不会。脚本使用Docker Volume持久化数据，更新只替换代码，不影响数据。

### Q: 如何回滚到旧版本？
A: 修改脚本中的镜像TAG，或使用备份的数据库文件。

### Q: 如何查看更新日志？
A: 运行 `docker logs -f king-detective`

## 安全建议

1. **不要**将包含敏感信息的环境变量写入脚本
2. **不要**将Token提交到GitHub
3. 建议在服务器上创建 `.env` 文件存储配置（不要提交到Git）

## 示例 .env 文件 (仅存于服务器，不提交)

```bash
# .env
BOT_TOKEN="你的Token"
ADMIN_USERNAME="你的用户名"
ADMIN_PASSWORD="你的密码"
```

然后运行:
```bash
source .env && bash update.sh
```
