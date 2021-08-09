#!/usr/bin/env bash

sed -i "s|MANAGER1|${MANAGER1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|MANAGER2|${MANAGER2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|MANAGER3|${MANAGER3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf

sed -i "s|UI1|${UI1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|UI2|${UI2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|UI3|${UI3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf


sed -i "s|VERIFICATION1|${VERIFICATION1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|VERIFICATION2|${VERIFICATION2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|VERIFICATION3|${VERIFICATION3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf