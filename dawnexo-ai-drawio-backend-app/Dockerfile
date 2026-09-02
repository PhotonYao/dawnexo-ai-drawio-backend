# ==============================================================
# AI-DrawIO 后端服务运行镜像（仅装载 boot jar）
#
# 前置：先在后端工程根目录完成打包，生成 boot jar（DDD 仅需 app 模块产物，
#       其余模块已作为依赖打进这个 fat jar）：
#   mvn clean package -Pprod -DskipTests
#
# 构建（在本目录 dawnexo-ai-drawio-backend-app/ 下执行）：
#   docker build -t dawnexo-ai-drawio-backend:1.0 .
# ==============================================================

# 运行镜像源：默认走毫秒云（docker.1ms.run）加速国内拉取；
# 需直连 Docker Hub 时构建加 --build-arg JRE_IMAGE=eclipse-temurin:17-jre-alpine
ARG JRE_IMAGE=docker.1ms.run/library/eclipse-temurin:17-jre-alpine
FROM ${JRE_IMAGE}

ARG APP_VERSION=1.0
LABEL org.opencontainers.image.title="dawnexo-ai-drawio-backend" \
      org.opencontainers.image.description="AI DrawIO 智能绘图平台后端服务（Spring Boot 3 / Java 17）" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.authors="kangyaocoding"

# 时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    apk del tzdata

# 默认以 prod 配置启动，可被 docker-compose 的 environment 覆盖
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8090 \
    JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

WORKDIR /app

# 非 root 用户运行；日志目录挂载点
RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /app/data/log && chown -R app:app /app

# 仅拷贝 boot jar，源码与其余构建产物不进入镜像
COPY target/ai-agent-scaffold-app.jar /app/app.jar

# 技能目录以真实文件路径进入镜像：
# fat jar 内的 classpath 资源无法按目录读取，容器内由 SKILLS_TYPE=directory
# 指向此目录加载（见 docker-compose.yml 与 agent-draw-io.yml）
COPY --chown=app:app src/main/resources/agent/skills /app/agent/skills

USER app

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
