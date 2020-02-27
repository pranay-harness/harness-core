#!/bin/bash
set -x

cp /opt/harness/proxy/scripts/harness-on-prem-proxy.conf /etc/nginx/conf.d/harness-on-prem-proxy.conf

bash /opt/harness/proxy/scripts/replace_parameters.sh
/usr/sbin/nginx -g "daemon off;"