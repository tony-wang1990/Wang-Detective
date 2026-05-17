#!/bin/sh
set -e

APP_DIR="/app/king-detective"
RUNTIME_DIR="$APP_DIR/runtime"
TRIGGER_FILE="$RUNTIME_DIR/update_version_trigger.flag"
HEARTBEAT_FILE="$RUNTIME_DIR/watcher_heartbeat"
SERVICE_NAME="${KING_DETECTIVE_SERVICE:-king-detective}"
IMAGE="${KING_DETECTIVE_IMAGE:-ghcr.io/tony-wang1990/wang-detective:main}"
REPOSITORY="${KING_DETECTIVE_GITHUB_REPOSITORY:-tony-wang1990/Wang-Detective}"
BRANCH="${KING_DETECTIVE_GITHUB_BRANCH:-main}"
HEALTH_URL="${KING_DETECTIVE_HEALTH_URL:-http://king-detective:9527/actuator/health}"

echo "安装 watcher 工具 (docker compose, curl, sqlite)..."
apk add --no-cache docker-cli-compose curl sqlite >/dev/null

mkdir -p "$RUNTIME_DIR"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "King-Detective Watcher"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "应用目录: $APP_DIR"
echo "目标镜像: $IMAGE"
echo "代码仓库: $REPOSITORY"
echo "监控文件: $TRIGGER_FILE"
echo "健康检查: $HEALTH_URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

latest_version() {
  latest_sha="$(curl -fsS "https://api.github.com/repos/$REPOSITORY/commits/$BRANCH" 2>/dev/null \
    | grep -m1 '"sha":' \
    | sed -E 's/.*"sha"[[:space:]]*:[[:space:]]*"([0-9a-fA-F]{7}).*/\1/' || true)"
  if [ -n "$latest_sha" ]; then
    printf '%s-%s' "$BRANCH" "$latest_sha"
  else
    printf '%s' "$BRANCH"
  fi
}

update_db_version() {
  db_file="$APP_DIR/data/king-detective.db"
  version_value="$(latest_version)"

  if [ ! -f "$db_file" ]; then
    echo "数据库文件不存在，跳过版本号写入: $db_file"
    return
  fi

  escaped_version="$(printf '%s' "$version_value" | sed "s/'/''/g")"
  record_exists="$(sqlite3 "$db_file" "SELECT COUNT(*) FROM oci_kv WHERE code = 'Y106' AND type = 'Y003';" 2>/dev/null || echo "0")"

  if [ "$record_exists" -gt 0 ] 2>/dev/null; then
    sqlite3 "$db_file" "UPDATE oci_kv SET value = '$escaped_version' WHERE code = 'Y106' AND type = 'Y003';"
  else
    kv_id="$(date +%s)"
    sqlite3 "$db_file" "INSERT INTO oci_kv (id, code, type, value) VALUES ('$kv_id', 'Y106', 'Y003', '$escaped_version');" 2>/dev/null || true
  fi
  echo "版本号记录已更新为: $version_value"
}

wait_for_health() {
  echo "等待新版本就绪..."
  for i in $(seq 1 90); do
    container_state="$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$SERVICE_NAME" 2>/dev/null || true)"
    if echo "$container_state" | grep -q "running healthy"; then
      echo "服务健康检查通过"
      return 0
    fi
    if curl -fsS --max-time 3 "$HEALTH_URL" 2>/dev/null | grep -q '"status":"UP"'; then
      echo "服务健康检查通过"
      return 0
    fi
    echo "  - 启动中... $((i * 5))s/450s $container_state"
    sleep 5
  done

  echo "服务未在预期时间内变为健康，请查看 docker logs $SERVICE_NAME"
  return 1
}

run_update() {
  cd "$APP_DIR"

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "检测到更新触发器: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  : > "$TRIGGER_FILE"

  echo "[1/4] 拉取最新镜像: $IMAGE"
  docker compose pull "$SERVICE_NAME"

  echo "[2/4] 重建应用容器"
  docker compose up -d --force-recreate "$SERVICE_NAME"

  echo "[3/4] 等待服务恢复"
  wait_for_health || true

  echo "[4/4] 写入版本记录"
  update_db_version || true

  echo "更新流程完成"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

while true; do
  date '+%Y-%m-%d %H:%M:%S' > "$HEARTBEAT_FILE" 2>/dev/null || true

  if [ -f "$TRIGGER_FILE" ]; then
    content="$(cat "$TRIGGER_FILE" 2>/dev/null | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
    if [ -n "$content" ]; then
      run_update
    fi
  fi

  sleep 2
done
