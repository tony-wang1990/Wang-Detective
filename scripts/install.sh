#!/bin/bash

echo "=== King-Detective 安装脚本 ==="
echo "步骤 1: 检查环境..."

# 检查必要的命令
command -v wget >/dev/null 2>&1 || { echo "错误: 未安装 wget"; exit 1; }
# 检查并安装 Docker
if ! command -v docker &> /dev/null; then
    echo "未检测到 Docker，正在尝试自动安装..."
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        if [[ "$ID" == "ubuntu" || "$ID" == "debian" || "$ID" == "centos" || "$ID" == "ol" ]]; then
            curl -fsSL https://get.docker.com | bash
            systemctl enable docker
            systemctl start docker
        else
            echo "错误: 不支持的操作系统 $ID，请手动安装 Docker"
            exit 1
        fi
    else
        echo "错误: 无法检测操作系统版本，请手动安装 Docker"
        exit 1
    fi
else
    echo "  - Docker 已安装"
fi

# 检查并安装 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "未检测到 docker-compose，正在尝试安装..."
    # 尝试作为 Docker 插件安装
    if command -v docker &> /dev/null; then
         echo "  - 尝试安装 Docker Compose 插件..."
         if [[ -f /etc/os-release ]]; then
            . /etc/os-release
            if [[ "$ID" == "ubuntu" || "$ID" == "debian" ]]; then
                apt-get update && apt-get install -y docker-compose-plugin
            elif [[ "$ID" == "centos" || "$ID" == "ol" ]]; then
                yum install -y docker-compose-plugin
            fi
        fi
    fi
    
    # 如果插件安装失败或仍无法通过 docker-compose 命令调用（为了兼容旧习惯，我们创建一个别名或安装独立二进制）
    if ! docker compose version &> /dev/null; then
         # 下载独立二进制文件 (兼容 ARM64 和 AMD64)
         ARCH=$(uname -m)
         if [[ "$ARCH" == "aarch64" ]]; then
             curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-aarch64" -o /usr/local/bin/docker-compose
         else
             curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
         fi
         chmod +x /usr/local/bin/docker-compose
    fi
fi

# 再次检查
if ! command -v docker &> /dev/null; then echo "错误: Docker 安装失败"; exit 1; fi
if ! command -v docker-compose &> /dev/null; then 
    # 如果没有 docker-compose 命令，但有 docker compose 插件，创建一个别名函数或者提示用户
    if docker compose version &> /dev/null; then
        echo "  - 检测到 Docker Compose Plugin (docker compose)"
        # 创建一个临时的 docker-compose 别名给当前脚本使用
        docker-compose() { docker compose "$@"; }
        export -f docker-compose
    else
        echo "错误: Docker Compose 安装失败"; exit 1; 
    fi
else
    echo "  - docker-compose 已安装"
fi

echo "步骤 2: 创建目录..."
mkdir -p /app/king-detective/keys || { echo "错误: 无法创建目录"; exit 1; }
cd /app/king-detective || { echo "错误: 无法进入目录"; exit 1; }

echo "步骤 3: 下载配置文件..."

# 只在文件不存在时才下载，避免覆盖用户配置
if [ ! -f "docker-compose.yml" ]; then
    wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose.yml || { echo "错误: 下载 docker-compose.yml 失败"; exit 1; }
    echo "  - docker-compose.yml 下载成功"
else
    echo "  - docker-compose.yml 已存在，跳过下载"
fi

if [ ! -f "application.yml" ]; then
    wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml || { echo "错误: 下载 application.yml 失败"; exit 1; }
    echo "  - application.yml 下载成功"
else
    echo "  - application.yml 已存在，保留现有配置"
fi

if [ ! -f "king-detective.db" ]; then
    wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/king-detective.db || { echo "错误: 下载 king-detective.db 失败"; exit 1; }
    echo "  - king-detective.db 下载成功"
else
    echo "  - king-detective.db 已存在，保留现有数据"
fi

echo "步骤 4: 拉取最新镜像..."
docker-compose pull || { echo "警告: 拉取镜像失败，将使用现有镜像"; }

echo "步骤 5: 启动服务..."
docker-compose up -d --force-recreate || { echo "错误: 启动服务失败"; exit 1; }

echo ""
echo "=== 安装完成！ ==="
echo "访问地址: http://$(curl -s ifconfig.me):9527"
echo "默认账号: admin / admin123456"