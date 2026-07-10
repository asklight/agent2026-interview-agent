#!/usr/bin/env bash
set -euo pipefail

: "${TJU_VPN_HOST:=vpn.tju.edu.cn}"
: "${TJU_VPN_USERNAME:?TJU_VPN_USERNAME is required}"
: "${TJU_VPN_PASSWORD:?TJU_VPN_PASSWORD is required}"
: "${TJU_VPN_RECONNECT_SECONDS:=3600}"

if [ ! -c /dev/net/tun ]; then
  mkdir -p /dev/net
  mknod /dev/net/tun c 10 200
  chmod 0666 /dev/net/tun
fi

cat >/etc/tinyproxy/tinyproxy.conf <<'EOF'
User root
Group root
Port 8888
Listen 0.0.0.0
Timeout 600
LogFile "/dev/stdout"
LogLevel Notice
PidFile "/tmp/tinyproxy.pid"
MaxClients 100
Allow 127.0.0.1
Allow 10.0.0.0/8
Allow 172.16.0.0/12
Allow 192.168.0.0/16
ViaProxyName "agent2026-ecvpn"
ConnectPort 443
ConnectPort 563
EOF

monitor_pid=""
proxy_pid=""

stop_cycle() {
  [ -n "$proxy_pid" ] && kill "$proxy_pid" 2>/dev/null || true
  [ -n "$monitor_pid" ] && kill "$monitor_pid" 2>/dev/null || true
  wait "$proxy_pid" 2>/dev/null || true
  wait "$monitor_pid" 2>/dev/null || true
  proxy_pid=""
  monitor_pid=""
  pkill -TERM -f '/usr/share/sangfor/EasyConnect/resources/bin/EasyMonitor' 2>/dev/null || true
}

repair_docker_route() {
  local cidr prefix network
  cidr=$(ip -o -4 addr show dev eth0 | awk '{print $4}' | head -n 1)
  prefix=$(printf '%s\n' "$cidr" | awk -F'[./]' '{print $1 "." $2 "."}')
  network=$(printf '%s\n' "$cidr" | awk -F'[./]' '{print $1 "." $2 ".0.0/16"}')
  if [ -n "$cidr" ] && [ -n "$prefix" ] && [ -n "$network" ]; then
    ip route show dev tun0 | awk -v prefix="$prefix" '$1 ~ ("^" prefix) { print $1 }' | while read -r route; do
      ip route del "$route" dev tun0 2>/dev/null || true
    done
    ip route replace "$network" dev eth0
  fi
}

trap 'stop_cycle; exit 0' INT TERM
while true; do
  rm -f /tmp/tju-vpn.twfid
  /usr/share/sangfor/EasyConnect/resources/bin/EasyMonitor &
  monitor_pid=$!
  for _ in $(seq 1 30); do
    curl -k -sS --connect-timeout 1 --max-time 2 'https://127.0.0.1:54530/ECAgent?op=InitECAgent' >/dev/null 2>&1 && break
    sleep 1
  done
  if ! node /opt/ecvpn/vpn-connect.js; then
    stop_cycle
    sleep 30
    continue
  fi
  repair_docker_route
  tinyproxy -d -c /etc/tinyproxy/tinyproxy.conf &
  proxy_pid=$!
  echo "EasyConnect connected; scheduled reconnect in ${TJU_VPN_RECONNECT_SECONDS}s."
  sleep "$TJU_VPN_RECONNECT_SECONDS" || true
  echo "Reconnecting EasyConnect after scheduled interval."
  stop_cycle
  sleep 2
done
