#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


echo "Running hostname:port connectivity test for on-prem"

host=127.0.0.1
port=22

if echo "$(uname -n)" 2>/dev/null > /dev/tcp/"$host"/"$port"
then
    echo success at "$host":"$port"
else
    echo failure at "$host":"$port"
fi
