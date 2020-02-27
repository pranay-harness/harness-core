#!/bin/bash
set -x

cp /opt/harness/proxy/pov_scripts/harness-on-prem-pov-proxy.conf /etc/nginx/conf.d/harness-on-prem-pov-proxy.conf
bash /opt/harness/proxy/pov_scripts/replace_parameters.sh
/usr/sbin/nginx -g "daemon off;"