#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
BACKUP_DIR="${BACKUP_DIR:-backups}"

if [ ! -f "$ENV_FILE" ]; then
  echo "$ENV_FILE is missing in deployment directory" >&2
  exit 1
fi

umask 077

env_value() {
  local name="$1"
  awk -v key="$name" '
    index($0, key "=") == 1 { value = substr($0, length(key) + 2) }
    END { sub(/\r$/, "", value); print value }
  ' "$ENV_FILE"
}

if [ -z "$(env_value RESOURCE_TOKEN_SECRET)" ]; then
  generated_secret="$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')"
  if grep -q '^RESOURCE_TOKEN_SECRET=' "$ENV_FILE"; then
    temporary_env="$(mktemp)"
    awk -v replacement="RESOURCE_TOKEN_SECRET=$generated_secret" '
      /^RESOURCE_TOKEN_SECRET=/ { print replacement; next }
      { print }
    ' "$ENV_FILE" > "$temporary_env"
    mv "$temporary_env" "$ENV_FILE"
  else
    printf '\nRESOURCE_TOKEN_SECRET=%s\n' "$generated_secret" >> "$ENV_FILE"
  fi
  unset generated_secret
  echo "Generated and persisted a new RESOURCE_TOKEN_SECRET."
fi

chmod 600 "$ENV_FILE"

required_variables=(
  MYSQL_PASSWORD
  MYSQL_ROOT_PASSWORD
  RESOURCE_TOKEN_SECRET
  TJU_LLM_API_URL
  TJU_LLM_API_KEY
)

for name in "${required_variables[@]}"; do
  value="$(env_value "$name")"
  if [ -z "$value" ] || [[ "$value" == replace_with* ]]; then
    echo "$name is missing or still uses an example placeholder in $ENV_FILE" >&2
    exit 1
  fi
done

resource_secret="$(env_value RESOURCE_TOKEN_SECRET)"
if [ "$(printf '%s' "$resource_secret" | wc -c)" -lt 32 ]; then
  echo "RESOURCE_TOKEN_SECRET must contain at least 32 UTF-8 bytes" >&2
  exit 1
fi
unset resource_secret value

compose_args=(-f docker-compose.prod.yml)
if grep -Eiq '^ENABLE_TJU_VPN=(true|1|yes)$' "$ENV_FILE"; then
  for name in TJU_VPN_USERNAME TJU_VPN_PASSWORD; do
    value="$(env_value "$name")"
    if [ -z "$value" ] || [[ "$value" == replace_with* ]]; then
      echo "$name is required when ENABLE_TJU_VPN=true" >&2
      exit 1
    fi
  done

  sudo modprobe tun || true
  if [ ! -c /dev/net/tun ]; then
    sudo mkdir -p /dev/net
    sudo mknod /dev/net/tun c 10 200
    sudo chmod 0666 /dev/net/tun
  fi
  compose_args+=(-f docker-compose.vpn.yml)
fi

mkdir -p "$BACKUP_DIR"
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'predeploy-*.sql' -mtime +14 -delete
mysql_container="$(docker compose -f docker-compose.prod.yml --env-file "$ENV_FILE" ps -q mysql)"
if [ -n "$mysql_container" ] && [ "$(docker inspect -f '{{.State.Running}}' "$mysql_container")" = "true" ]; then
  backup_file="$BACKUP_DIR/predeploy-$(date -u +%Y%m%dT%H%M%SZ).sql"
  echo "Creating pre-deploy MySQL backup at $backup_file"
  if ! docker exec "$mysql_container" sh -c \
      'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
      > "$backup_file"; then
    rm -f "$backup_file"
    echo "MySQL backup failed; deployment aborted before starting the new application." >&2
    exit 1
  fi
  test -s "$backup_file"
else
  echo "No running MySQL container found; treating this as a first deployment."
fi

docker compose "${compose_args[@]}" --env-file "$ENV_FILE" config >/dev/null
timeout --foreground 20m docker compose "${compose_args[@]}" --env-file "$ENV_FILE" up -d --build --remove-orphans

wait_for_healthy() {
  local service="$1"
  local container_id status
  container_id="$(docker compose "${compose_args[@]}" --env-file "$ENV_FILE" ps -q "$service")"
  if [ -z "$container_id" ]; then
    echo "$service container was not created" >&2
    return 1
  fi

  for _ in $(seq 1 36); do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")"
    if [ "$status" = "healthy" ]; then
      echo "$service is healthy."
      return 0
    fi
    if [ "$status" = "unhealthy" ] || [ "$status" = "exited" ] || [ "$status" = "dead" ]; then
      echo "$service entered terminal state: $status" >&2
      return 1
    fi
    sleep 5
  done

  echo "Timed out waiting for $service to become healthy" >&2
  return 1
}

if ! wait_for_healthy server || ! wait_for_healthy web; then
  docker compose "${compose_args[@]}" --env-file "$ENV_FILE" ps >&2
  exit 1
fi

docker compose "${compose_args[@]}" --env-file "$ENV_FILE" ps
docker image prune -f
