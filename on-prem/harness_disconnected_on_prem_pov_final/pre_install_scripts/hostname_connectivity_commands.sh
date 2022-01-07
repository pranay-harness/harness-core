#!/bin/bash
# Copyright 2019 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

echo "Running hostname:port connectivity test for on-prem"

host=127.0.0.1
port=22

if echo "$(uname -n)" 2>/dev/null > /dev/tcp/"$host"/"$port"
then
    echo success at "$host":"$port"
else
    echo failure at "$host":"$port"
fi
