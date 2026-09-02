@echo off
setlocal
cd /d "%~dp0"

REM ==============================================================
REM  后端镜像构建并推送（腾讯云 CCR 等私有镜像仓库通用）
REM  用法：双击运行，或在本目录的 cmd / PowerShell / Git Bash 中执行 build-push.bat
REM
REM  项目结构：dawnexo-ai-drawio-backend（Maven 多模块 DDD）
REM    dawnexo-ai-drawio-backend-app\            ← Dockerfile 所在模块
REM      target\ai-agent-scaffold-app.jar        ← boot jar（镜像只装它）
REM
REM  依赖：1) 已完成打包：本目录执行 mvn clean package -Pprod -DskipTests
REM        2) .env（本目录或上级 my-ai-drawio 目录）中的 BACKEND_IMAGE
REM
REM  登录：.env 同时配置 REGISTRY、REGISTRY_USERNAME、REGISTRY_PASSWORD 时静默登录；
REM        否则先直接推送，认证被拒时自动进入交互式 docker login（手动输入密码）后重试
REM  注意：REGISTRY_PASSWORD 含 cmd 特殊字符时请改用交互输入（留空）或使用 build-push.sh
REM  注意：本文件必须保持 GBK（ANSI）编码与 CRLF 换行，用记事本编辑即可，勿存为 UTF-8
REM ==============================================================

REM ---- 定位 .env（优先本目录，其次上级 my-ai-drawio 目录） ----
set "ENV_FILE=%~dp0.env"
if not exist "%ENV_FILE%" set "ENV_FILE=%~dp0..\.env"
if not exist "%ENV_FILE%" (
    echo 错误：未找到 .env（应位于本目录或上级 my-ai-drawio 目录）
    goto :fail
)

for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do set "ENV_%%a=%%b"

if not defined ENV_BACKEND_IMAGE (
    echo 错误：.env 缺少 BACKEND_IMAGE
    goto :fail
)

REM ---- 校验 boot jar 已打包（镜像只装这一个文件） ----
set "BACKEND_APP_DIR=%~dp0dawnexo-ai-drawio-backend-app"
set "BACKEND_JAR=%BACKEND_APP_DIR%\target\ai-agent-scaffold-app.jar"
if not exist "%BACKEND_JAR%" (
    echo 错误：未找到 boot jar：%BACKEND_JAR%
    echo 请先在本目录执行：mvn clean package -Pprod -DskipTests
    goto :fail
)

echo ==^> 构建后端镜像: %ENV_BACKEND_IMAGE%
docker build -t "%ENV_BACKEND_IMAGE%" "%BACKEND_APP_DIR%"
if errorlevel 1 goto :fail

call :push_image "%ENV_BACKEND_IMAGE%" || goto :fail

echo.
echo 完成。服务器上执行：
if defined ENV_REGISTRY echo   docker login %ENV_REGISTRY%    （首次需要，按提示输入密码）
echo   docker compose pull ^&^& docker compose up -d
echo.
pause
exit /b 0

REM ---- 推送单个镜像：认证被拒时登录后重试 ----
:push_image
docker push "%~1"
if not errorlevel 1 exit /b 0
echo ==^> 推送被拒，需要登录后重试
call :do_login || exit /b 1
docker push "%~1"
if errorlevel 1 exit /b 1
exit /b 0

REM ---- 登录镜像仓库：有凭证静默登录，无凭证交互式输入 ----
:do_login
if not defined ENV_REGISTRY exit /b 1
if defined ENV_REGISTRY_USERNAME if defined ENV_REGISTRY_PASSWORD goto :login_silent
if defined ENV_REGISTRY_USERNAME goto :login_user
echo ==^> 登录 %ENV_REGISTRY%（请按提示输入用户名与密码）
docker login "%ENV_REGISTRY%"
exit /b 0

:login_silent
echo ==^> 使用 .env 凭证登录 %ENV_REGISTRY%
echo %ENV_REGISTRY_PASSWORD%| docker login "%ENV_REGISTRY%" --username "%ENV_REGISTRY_USERNAME%" --password-stdin
exit /b 0

:login_user
echo ==^> 登录 %ENV_REGISTRY%（用户名 %ENV_REGISTRY_USERNAME%，请按提示输入密码）
docker login "%ENV_REGISTRY%" --username "%ENV_REGISTRY_USERNAME%"
exit /b 0

:fail
echo.
echo 操作失败，已中止。
pause
exit /b 1
