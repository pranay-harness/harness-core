#!/bin/bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

set -x

cp /opt/harness/proxy/scripts/harness-on-prem-proxy.conf /etc/nginx/conf.d/harness-on-prem-proxy.conf

bash /opt/harness/proxy/scripts/replace_parameters.sh
/usr/sbin/nginx -g "daemon off;"
