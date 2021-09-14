#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

set -x

cp /opt/harness/proxy/scripts/harness-on-prem-proxy.conf /etc/nginx/conf.d/harness-on-prem-proxy.conf

bash /opt/harness/proxy/scripts/replace_parameters.sh
/usr/sbin/nginx -g "daemon off;"
