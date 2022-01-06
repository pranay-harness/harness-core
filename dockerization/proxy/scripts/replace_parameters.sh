#!/usr/bin/env bash
# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

sed -i "s|MANAGER1|${MANAGER1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|MANAGER2|${MANAGER2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|MANAGER3|${MANAGER3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf

sed -i "s|UI1|${UI1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|UI2|${UI2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|UI3|${UI3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf


sed -i "s|VERIFICATION1|${VERIFICATION1}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|VERIFICATION2|${VERIFICATION2}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
sed -i "s|VERIFICATION3|${VERIFICATION3}|" /etc/nginx/conf.d/harness-on-prem-proxy.conf
