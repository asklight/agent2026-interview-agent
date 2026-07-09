# GitHub Actions 自动部署说明

本项目的生产部署方式是：推送 GitHub `master` 分支后触发 Actions，先执行后端测试和前端构建，再通过 SSH 同步代码到服务器，并在服务器上执行 Docker Compose 构建与启动。

## 1. GitHub Secrets

在 GitHub 仓库的 `Settings -> Secrets and variables -> Actions` 中添加：

| Secret | 说明 |
| --- | --- |
| `DEPLOY_HOST` | 服务器 IP 或域名 |
| `DEPLOY_USER` | SSH 用户，例如 `ubuntu` |
| `DEPLOY_SSH_PRIVATE_KEY` | 能登录服务器的 SSH 私钥内容 |
| `DEPLOY_PORT` | SSH 端口，默认可不填，工作流会使用 `22` |
| `DEPLOY_PATH` | 服务器部署目录，默认可不填，工作流会使用 `/opt/agent2026-interview-agent` |

不要把服务器密码、私钥、学校 API Key、数据库密码、VPN 账号密码提交到仓库。

## 2. 初始化服务器

服务器需要安装 Docker、Docker Compose 插件和 rsync。首次部署前可以执行：

```bash
scp deploy/bootstrap-server.sh ubuntu@服务器IP:/tmp/bootstrap-agent2026.sh
ssh ubuntu@服务器IP "DEPLOY_PATH=/opt/agent2026-interview-agent bash /tmp/bootstrap-agent2026.sh"
```

如果脚本执行后提示需要重新登录，退出 SSH 后重新登录一次。

## 3. 生产环境变量

在服务器部署目录创建 `.env.prod`：

```bash
cd /opt/agent2026-interview-agent
cp deploy/env.prod.example .env.prod
vim .env.prod
chmod 600 .env.prod
```

必须填写：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `TJU_LLM_API_URL`
- `TJU_LLM_API_KEY`
- `TJU_LLM_MODEL`

`.env.prod` 只保存在服务器，不进入 Git 仓库。

## 4. VPN 隔离模式

如果服务器不能直连学校 `tju-llm` API，可以启用隔离的 EasyConnect sidecar。这个 sidecar 只影响自己的容器网络，不会改宿主机默认路由，因此不会影响 SSH 和公网 Web 入口。

不要再在宿主机上直接启动 EasyConnect 隧道。宿主机只保留 Docker、SSH、Web 入口，学校 API 的出站访问交给 `ecvpn` 容器。

在服务器 `.env.prod` 中设置：

```env
ENABLE_TJU_VPN=true
TJU_VPN_HOST=vpn.tju.edu.cn
TJU_VPN_USERNAME=你的 VPN 用户名
TJU_VPN_PASSWORD=你的 VPN 密码
```

启用后，GitHub Actions 会自动使用：

```bash
docker-compose.prod.yml + docker-compose.vpn.yml
```

后端 `server` 容器会通过 `ecvpn:8888` 代理访问学校 API。默认部署不启用 VPN。

手动验证命令：

```bash
cd /opt/agent2026-interview-agent
docker compose -f docker-compose.prod.yml -f docker-compose.vpn.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml -f docker-compose.vpn.yml --env-file .env.prod ps
```

关闭 VPN 隔离模式时，把 `.env.prod` 中的 `ENABLE_TJU_VPN=false`，然后重新部署即可。

## 5. 手动验证部署

```bash
cd /opt/agent2026-interview-agent
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

访问：

```text
http://服务器IP/
http://服务器IP/api/health
http://服务器IP/api/health/db
```

## 6. 分支规则

本地开发主分支是 `main`。推送到 GitHub 时使用：

```bash
git push github main:master
```

GitHub Actions 监听 GitHub 的 `master` 分支。学校 GitLab 使用 `main` 分支：

```bash
git push origin main
```
