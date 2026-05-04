#!/bin/sh
set -e

# 安装必要工具
echo "📦 安装必要工具 (docker-cli, curl, sqlite)..."
if ! command -v docker &> /dev/null; then
    # Docker CLI 未安装，一起安装所有工具
    apk add --no-cache docker-cli curl sqlite
else
    # Docker CLI 已安装，只安装其他工具
    apk add --no-cache curl sqlite 2>/dev/null || true
fi
echo "✅ 工具安装完成"


echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 King-Detective Watcher v1.0"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📂 监控目录: /app/king-detective"
echo "🔔 监控文件: update_version_trigger.flag"
echo "⏰ 检测间隔: 2秒"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

TRIGGER_FILE="/app/king-detective/update_version_trigger.flag"

while true; do
  if [ -f "$TRIGGER_FILE" ]; then
    content=$(cat "$TRIGGER_FILE" 2>/dev/null | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')
    
    if [ "$content" = "trigger" ]; then
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] 检测到更新触发器！"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      
      # 清空trigger文件
      > "$TRIGGER_FILE"
      
      cd /app/king-detective
      
      echo "🛑 [1/5] 停止king-detective容器..."
      docker compose stop king-detective || {
        echo "⚠️ 停止容器失败，可能已经停止"
      }
      
      echo "🧹 [2/5] 清理旧镜像..."
      OLD_IMAGE=$(docker images | grep "king-detective" | grep -v "watcher" | awk 'NR==1{print $3}')
      if [ -n "$OLD_IMAGE" ]; then
        docker rmi -f "$OLD_IMAGE" 2>/dev/null || echo "  ↳ 旧镜像使用中，跳过删除"
      else
        echo "  ↳ 未找到旧镜像"
      fi
      
      echo "⬇️ [3/5] 拉取最新镜像 (ghcr.io/tony-wang1990/king-detective:main)..."
      if docker compose pull king-detective; then
        echo "  ↳ ✅ 镜像拉取成功"
      else
        echo "  ↳ ❌ 镜像拉取失败，更新中止"
        docker compose up -d king-detective
        continue
      fi
      
      echo "🚀 [4/5] 启动新版本..."
      docker compose up -d king-detective
      
      # 等待容器启动
      echo "  ↳ 等待容器启动..."
      sleep 5
      
      # 检查容器状态
      if docker ps | grep -q "king-detective"; then
        echo "  ↳ ✅ 容器启动成功"
      else
        echo "  ↳ ⚠️ 容器未运行，请检查日志"
      fi
      
      echo "📦 [5/5] 更新版本号记录..."
      LATEST_TAG=$(curl -s https://api.github.com/repos/tony-wang1990/king-detective/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/' 2>/dev/null || echo "")
      
      if [ -n "$LATEST_TAG" ] && [ "$LATEST_TAG" != "null" ]; then
        echo "  ↳ GitHub最新版本: $LATEST_TAG"
        
        # 更新数据库版本号
        DB_FILE="/app/king-detective/king-detective.db"
        if [ -f "$DB_FILE" ]; then
          RECORD_EXISTS=$(sqlite3 "$DB_FILE" "SELECT COUNT(*) FROM oci_kv WHERE code = 'Y106' AND type = 'Y003';" 2>/dev/null || echo "0")
          
          if [ "$RECORD_EXISTS" -gt 0 ]; then
            sqlite3 "$DB_FILE" "UPDATE oci_kv SET value = '$LATEST_TAG' WHERE code = 'Y106' AND type = 'Y003';"
            echo "  ↳ ✅ 数据库版本号已更新为: $LATEST_TAG"
          else
            # 插入新记录
            sqlite3 "$DB_FILE" "INSERT INTO oci_kv (code, type, value) VALUES ('Y106', 'Y003', '$LATEST_TAG');" 2>/dev/null && \
              echo "  ↳ ✅ 已插入版本号记录" || \
              echo "  ↳ ⚠️ 版本号记录操作失败"
          fi
        else
          echo "  ↳ ⚠️ 数据库文件不存在，跳过版本号更新"
        fi
      else
        echo "  ↳ ⚠️ 无法获取最新Release版本号"
      fi
      
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo "✅ 更新完成！服务已重启"
      echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
      echo ""
    fi
  fi
  
  sleep 2
done
