#!/usr/bin/env bash
# ==============================================================
# 后端镜像构建并推送（腾讯云 CCR 等私有镜像仓库通用）
# 用法：在本目录（dawnexo-ai-drawio-backend/）执行  ./build-push.sh
#       （绝对路径定位，在任意工作目录执行均可）
#
# 项目结构：dawnexo-ai-drawio-backend/（Maven 多模块 DDD）
#   └── dawnexo-ai-drawio-backend-app/          ← Dockerfile 所在模块
#       └── target/ai-agent-scaffold-app.jar    ← boot jar（镜像只装它）
#
# 依赖：1) 已完成打包：本目录执行 mvn clean package -Pprod -DskipTests
#       2) .env（本目录或上级 my-ai-drawio 目录）中的 BACKEND_IMAGE
#
# 登录：.env 同时配置 REGISTRY + REGISTRY_USERNAME + REGISTRY_PASSWORD 时静默登录；
#       否则先直接推送，认证被拒时自动进入交互式 docker login（手动输入密码）后重试
# ==============================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---- 定位 .env（优先本目录，其次上级 my-ai-drawio 目录） ----
if [ -f "$SCRIPT_DIR/.env" ]; then
  ENV_FILE="$SCRIPT_DIR/.env"
elif [ -f "$SCRIPT_DIR/../.env" ]; then
  ENV_FILE="$SCRIPT_DIR/../.env"
else
  echo "错误：未找到 .env（应位于本目录或上级 my-ai-drawio 目录）"; exit 1
fi
get_env() { grep -E "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2- | tr -d '\r'; }

BACKEND_IMAGE=$(get_env BACKEND_IMAGE)
REGISTRY=$(get_env REGISTRY)
REGISTRY_USERNAME=$(get_env REGISTRY_USERNAME)
REGISTRY_PASSWORD=$(get_env REGISTRY_PASSWORD)

[ -z "$BACKEND_IMAGE" ] && { echo "错误：.env 缺少 BACKEND_IMAGE"; exit 1; }

# ---- 校验 boot jar 已打包（镜像只装这一个文件） ----
BACKEND_APP_DIR="$SCRIPT_DIR/dawnexo-ai-drawio-backend-app"
BACKEND_JAR="$BACKEND_APP_DIR/target/ai-agent-scaffold-app.jar"
if [ ! -f "$BACKEND_JAR" ]; then
  echo "错误：未找到 boot jar：$BACKEND_JAR"
  echo "请先在本目录执行：mvn clean package -Pprod -DskipTests"
  exit 1
fi

# ---- 登录镜像仓库：有凭证静默登录，无凭证交互式输入 ----
do_login() {
  [ -n "$REGISTRY" ] || return 1
  if [ -n "$REGISTRY_USERNAME" ] && [ -n "$REGISTRY_PASSWORD" ]; then
    echo "==> 使用 .env 凭证登录 $REGISTRY"
    echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" --username "$REGISTRY_USERNAME" --password-stdin
  elif [ -n "$REGISTRY_USERNAME" ]; then
    echo "==> 登录 $REGISTRY（用户名 $REGISTRY_USERNAME，请按提示输入密码）"
    docker login "$REGISTRY" --username "$REGISTRY_USERNAME"
  else
    echo "==> 登录 $REGISTRY（请按提示输入用户名与密码）"
    docker login "$REGISTRY"
  fi
}

# ---- 推送：认证被拒时登录后重试 ----
push_image() {
  echo "==> 推送 $1"
  if docker push "$1"; then
    return 0
  fi
  echo "==> 推送被拒，需要登录后重试"
  do_login
  docker push "$1"
}

echo "==> 构建后端镜像: $BACKEND_IMAGE"
docker build -t "$BACKEND_IMAGE" "$BACKEND_APP_DIR"

push_image "$BACKEND_IMAGE"

echo ""
echo "完成。服务器上执行："
[ -n "$REGISTRY" ] && echo "  docker login $REGISTRY   # 首次需要，按提示输入密码"
echo "  docker compose pull && docker compose up -d"
