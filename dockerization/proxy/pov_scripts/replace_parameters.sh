#!/usr/bin/env bash

sed -i "s|MANAGER1|${MANAGER1}|" /etc/nginx/conf.d/harness-on-prem-pov-proxy.conf

sed -i "s|VERIFICATION1|${VERIFICATION1}|" /etc/nginx/conf.d/harness-on-prem-pov-proxy.conf

sed -i "s|UI1|${UI1}|" /etc/nginx/conf.d/harness-on-prem-pov-proxy.conf
