#!/usr/bin/env bash
set -euo pipefail

: "${TJU_VPN_HOST:=vpn.tju.edu.cn}"
: "${TJU_VPN_USERNAME:?TJU_VPN_USERNAME is required}"
: "${TJU_VPN_PASSWORD:?TJU_VPN_PASSWORD is required}"

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
DefaultErrorFile "/usr/share/tinyproxy/default.html"
StatFile "/usr/share/tinyproxy/stats.html"
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

rm -f /tmp/tju-vpn.twfid

/usr/share/sangfor/EasyConnect/resources/bin/EasyMonitor &

for _ in $(seq 1 30); do
  if curl -k -sS --connect-timeout 1 --max-time 2 'https://127.0.0.1:54530/ECAgent?op=InitECAgent' >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

node /opt/ecvpn/vpn-connect.js

# EasyConnect adds broad routes for private networks, including Docker bridge
# subnets. Remove routes that overlap this container's eth0 /16 so peer
# containers can still reach the local proxy through Docker networking.
ETH0_CIDR=$(ip -o -4 addr show dev eth0 | awk '{print $4}' | head -n 1)
ETH0_PREFIX=$(printf '%s\n' "$ETH0_CIDR" | awk -F'[./]' '{print $1 "." $2 "."}')
if [ -n "$ETH0_CIDR" ] && [ -n "$ETH0_PREFIX" ]; then
  ip route show dev tun0 \
    | awk -v prefix="$ETH0_PREFIX" '$1 ~ ("^" prefix) { print $1 }' \
    | while read -r route; do
        ip route del "$route" dev tun0 2>/dev/null || true
      done
  ip route replace "$ETH0_CIDR" dev eth0
fi

exec tinyproxy -d -c /etc/tinyproxy/tinyproxy.conf
