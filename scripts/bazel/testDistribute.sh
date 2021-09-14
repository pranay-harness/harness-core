# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

> test-util.bzl
cat > test-util.bzl <<EOF
DISTRIBUTE_TESTING_WORKER=${DISTRIBUTE_TESTING_WORKER:-0}
DISTRIBUTE_TESTING_WORKERS=${DISTRIBUTE_TESTING_WORKERS:-1}
OPTIMIZED_PACKAGE_TESTS=${OPTIMIZED_PACKAGE_TESTS:-0}
EOF
