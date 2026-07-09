#!/usr/bin/env bash
set -euo pipefail

DEPLOY_PATH="${DEPLOY_PATH:-/opt/agent2026-interview-agent}"

sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg rsync

if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get install -y docker.io docker-compose-v2
fi

sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json >/dev/null <<'JSON'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.m.daocloud.io",
    "https://docker.1ms.run",
    "https://dockerproxy.net"
  ]
}
JSON

sudo systemctl enable --now docker
sudo systemctl restart docker
sudo usermod -aG docker "$USER"

sudo mkdir -p "$DEPLOY_PATH"
sudo chown -R "$USER:$USER" "$DEPLOY_PATH"

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose is not available. Install the Docker Compose plugin, then rerun deployment." >&2
  exit 1
fi

echo "Server bootstrap completed. Re-login may be required for docker group permissions."
