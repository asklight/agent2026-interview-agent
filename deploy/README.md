# GitHub Actions 自动部署说明

本项目的部署方式是：GitHub `master` 分支推送后触发 Actions，先执行后端测试和前端构建，再通过 SSH 同步代码到服务器，并在服务器上执行 Docker Compose 构建与启动。

## 1. GitHub Secrets

在 GitHub 仓库的 `Settings -> Secrets and variables -> Actions` 中添加：

| Secret | 说明 |
| --- | --- |
| `DEPLOY_HOST` | 服务器 IP 或域名 |
| `DEPLOY_USER` | SSH 用户，例如 `ubuntu` |
| `DEPLOY_SSH_PRIVATE_KEY` | 能登录服务器的 SSH 私钥内容 |
| `DEPLOY_PORT` | SSH 端口，默认可不填，工作流会使用 `22` |
| `DEPLOY_PATH` | 服务器部署目录，默认可不填，工作流会使用 `/opt/agent2026-interview-agent` |

不要把服务器密码、私钥、学校 API Key、数据库密码提交到仓库。

## 2. 初始化服务器

服务器需要安装 Docker、Docker Compose 插件和 rsync。首次部署前可以在服务器上执行：

```bash
scp deploy/bootstrap-server.sh ubuntu@服务器IP:/tmp/bootstrap-agent2026.sh
ssh ubuntu@服务器IP "DEPLOY_PATH=/opt/agent2026-interview-agent bash /tmp/bootstrap-agent2026.sh"
```

如果脚本执行后提示需要重新登录，退出 SSH 后重新登录一次。

## 3. 配置生产环境变量

在服务器部署目录创建 `.env.prod`：

```bash
cd /opt/agent2026-interview-agent
cp deploy/env.prod.example .env.prod
vim .env.prod
```

`.env.prod` 中必须填写：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `TJU_LLM_API_URL`
- `TJU_LLM_API_KEY`
- `TJU_LLM_MODEL`

`.env.prod` 只保存在服务器，不进入 Git 仓库。

## 4. 手动验证部署命令

如果需要在服务器上手动验证：

```bash
cd /opt/agent2026-interview-agent
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

访问：

```text
http://服务器IP/
http://服务器IP/api/health
```

## 5. 分支规则

本地开发主分支仍然是 `main`。推送到 GitHub 时使用：

```bash
git push github main:master
```

GitHub Actions 只监听 GitHub 的 `master` 分支。学校 GitLab 仍然推送 `main` 分支：

```bash
git push origin main
```
