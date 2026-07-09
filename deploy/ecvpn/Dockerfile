FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN set -eux; \
    printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d; \
    chmod +x /usr/sbin/policy-rc.d; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      iproute2 \
      libasound2 \
      libdbus-glib-1-2 \
      libgbm1 \
      libgtk2.0-0 \
      libnss3 \
      libx11-xcb1 \
      libxss1 \
      libxtst6 \
      nodejs \
      procps \
      tinyproxy \
      xvfb; \
    curl -fsSL -o /tmp/EasyConnect_x64.deb \
      http://download.sangfor.com.cn/download/product/sslvpn/pkg/linux_01/EasyConnect_x64.deb; \
    apt-get install -y /tmp/EasyConnect_x64.deb; \
    rm -rf /var/lib/apt/lists/* /tmp/EasyConnect_x64.deb

COPY deploy/ecvpn/ecvpn-entrypoint.sh /usr/local/bin/ecvpn-entrypoint.sh
COPY deploy/ecvpn/vpn-connect.js /opt/ecvpn/vpn-connect.js

RUN chmod 0755 /usr/local/bin/ecvpn-entrypoint.sh /opt/ecvpn/vpn-connect.js

EXPOSE 8888

ENTRYPOINT ["/usr/local/bin/ecvpn-entrypoint.sh"]
