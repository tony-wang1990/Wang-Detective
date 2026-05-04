# 📚 King-Detective API Documentation

## 概述

King-Detective提供RESTful API用于OCI实例管理和系统配置。

**Base URL**: `http://your-server:9527/api`  
**认证方式**: Bearer Token

---

## 认证

### 登录获取Token

```http
POST /api/sys/login
Content-Type: application/json

{
  "account": "admin",
  "password": "your_password",
  "mfaCode": "123456"  // 可选，如果启用了MFA
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "currentVersion": "2.0.0",
    "latestVersion": "2.0.0"
  }
}
```

**错误响应**:
```json
{
  "code": -1,
  "message": "账号或密码不正确"
}
```

---

## 系统配置

### 获取系统概览

```http
GET /api/sys/glance
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "apiConfigCount": 5,
    "enabledTaskCount": 12,
    "totalInstanceCount": 25,
    "runningInstanceCount": 20
  }
}
```

---

## 配置管理

### 获取配置列表

```http
GET /api/sys/cfgs
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Config 1",
      "region": "ap-tokyo-1",
      "createTime": "2026-01-01 00:00:00"
    }
  ]
}
```

### 添加配置

```http
POST /api/sys/cfgs
Authorization: Bearer <token>
Content-Type: multipart/form-data

{
  "name": "My OCI Config",
  "privateKeyFile": <file>,
  "configFile": <file>
}
```

---

## 实例管理

### 获取实例列表

```http
GET /api/instances
Authorization: Bearer <token>
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "ocid1.instance...",
      "displayName": "instance-1",
      "state": "RUNNING",
      "region": "ap-tokyo-1",
      "shape": "VM.Standard.E2.1.Micro"
    }
  ]
}
```

---

## 安全功能

### 检查防御模式状态

防御模式由Web拦截器自动检查，无需API调用。

**工作原理**:
1. 所有请求经过`AuthInterceptor`
2. 检查`oci_kv`表中的`defense_mode_enabled`键
3. 如果为`true`，返回403 Forbidden
4. 仅Telegram操作可以切换状态

### IP黑名单检查

IP黑名单由Web拦截器自动检查，无需API调用。

**工作原理**:
1. 从请求头获取真实IP地址
2. 查询`ip_blacklist`表
3. 如果IP在黑名单中，返回403 Forbidden
4. 登录失败5次自动加入黑名单

---

## 错误代码

| Code | 含义 |
|------|------|
| 200 | 成功 |
| -1 | 业务错误（查看message） |
| 401 | 未授权（Token无效/过期） |
| 403 | 禁止访问（IP黑名单/防御模式） |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 速率限制

**登录接口**: 10次/分钟 per IP

超过限制返回:
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试"
}
```

---

## Telegram Bot Commands

Telegram Bot提供更丰富的功能接口：

### 主要命令

- `/start` - 显示主菜单
- `/menu` - 显示主菜单
- `/help` - 帮助信息

### 功能菜单

通过Inline Keyboard操作：
- 实例管理（开机/关机/重启）
- 配置管理
- 任务管理
- 安全管理 🆕
  - 防御模式切换
  - IP黑名单查看
  - 清空黑名单
- 监控查询
- 网络管理

---

## WebSocket (未实现)

计划中的实时通知功能：
```
ws://your-server:9527/ws/notifications
```

---

## 示例代码

### Python

```python
import requests

# 登录
response = requests.post(
    'http://your-server:9527/api/sys/login',
    json={'account': 'admin', 'password': 'password'}
)
token = response.json()['data']['token']

# 获取实例列表
headers = {'Authorization': f'Bearer {token}'}
instances = requests.get(
    'http://your-server:9527/api/instances',
    headers=headers
).json()

print(instances)
```

### JavaScript

```javascript
// 登录
const response = await fetch('http://your-server:9527/api/sys/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ account: 'admin', password: 'password' })
});

const { token } = await response.json().data;

// 获取实例
const instances = await fetch('http://your-server:9527/api/instances', {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());
```

---

## 更多信息

- **部署指南**: `docs/DEPLOYMENT.md`
- **FAQ**: `docs/FAQ.md`
- **GitHub**: https://github.com/tony-wang1990/king-detective
