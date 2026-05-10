# Wang-Detective API 摘要

## 基础信息

- Web 地址：`http://your-server:9527`
- API Base：`http://your-server:9527/api`
- 认证方式：`Authorization: Bearer <token>`
- WebSocket 认证：query 参数 `token=<token>`

## 登录

```http
POST /api/sys/login
Content-Type: application/json
```

```json
{
  "account": "admin",
  "password": "your_password",
  "mfaCode": "123456"
}
```

## 系统诊断

```http
GET /api/v1/system/diagnostics
Authorization: Bearer <token>
```

用于部署后快速检查数据库、数据目录、密钥目录、日志文件、默认密码、Telegram Bot Token、OpenAI Key、磁盘和运行时信息。

## 健康检查

```http
GET /actuator/health
```

示例响应：

```json
{
  "status": "UP",
  "databaseConnectivity": true,
  "memoryStatus": true,
  "uptimeSeconds": 3600,
  "version": "4.1.1"
}
```

## 运维终端页面

```text
GET /ops-terminal.html
```

登录 Web 面板后打开该页面，页面会自动读取 `sessionStorage.token`。如果从新浏览器标签打开，也可以手动粘贴 token。

## SSH/SFTP API

### 创建 Web SSH 会话

```http
POST /api/ops/ssh/session
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "credential": {
    "host": "1.2.3.4",
    "port": 22,
    "username": "opc",
    "password": "optional",
    "privateKey": "-----BEGIN OPENSSH PRIVATE KEY-----\n...\n-----END OPENSSH PRIVATE KEY-----",
    "passphrase": "",
    "connectTimeoutSeconds": 10
  },
  "ttlMinutes": 15
}
```

响应中的 `websocketPath` 可用于连接终端：

```text
ws://your-server:9527/ops/ssh/terminal/{sessionId}?token=<token>
```

### 测试 SSH 连接

```http
POST /api/ops/ssh/test
Authorization: Bearer <token>
Content-Type: application/json
```

请求体：

```json
{
  "host": "1.2.3.4",
  "port": 22,
  "username": "opc",
  "password": "optional",
  "privateKey": "optional",
  "passphrase": "optional",
  "connectTimeoutSeconds": 10
}
```

### 执行单条命令

```http
POST /api/ops/ssh/exec
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "credential": {
    "host": "1.2.3.4",
    "port": 22,
    "username": "opc",
    "password": "optional",
    "privateKey": "optional"
  },
  "command": "uptime",
  "timeoutSeconds": 30
}
```

### 批量命令

```http
POST /api/ops/ssh/batch
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "command": "hostname && uptime",
  "timeoutSeconds": 30,
  "targets": [
    {
      "name": "tokyo-a1",
      "credential": {
        "host": "1.2.3.4",
        "port": 22,
        "username": "opc",
        "password": "optional"
      }
    }
  ]
}
```

### SFTP 操作

所有 SFTP 请求都使用同一类请求体：

```json
{
  "credential": {
    "host": "1.2.3.4",
    "port": 22,
    "username": "opc",
    "password": "optional",
    "privateKey": "optional"
  },
  "path": "/home/opc",
  "targetPath": "/home/opc/new-name",
  "content": "file content",
  "recursive": false
}
```

接口列表：

- `POST /api/ops/sftp/list`
- `POST /api/ops/sftp/read`
- `POST /api/ops/sftp/write`
- `POST /api/ops/sftp/mkdir`
- `POST /api/ops/sftp/delete`
- `POST /api/ops/sftp/rename`

## 常见错误码

| Code | 含义 |
| --- | --- |
| 200 | 成功 |
| -1 | 业务错误，查看 `message` |
| 401 | 未授权或 token 过期 |
| 403 | 禁止访问，例如 IP 黑名单或防御模式 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 服务端异常 |
