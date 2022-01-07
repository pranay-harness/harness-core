# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

> test-util.bzl
cat > test-util.bzl <<EOF
DISTRIBUTE_TESTING_WORKER=${DISTRIBUTE_TESTING_WORKER:-0}
DISTRIBUTE_TESTING_WORKERS=${DISTRIBUTE_TESTING_WORKERS:-1}
OPTIMIZED_PACKAGE_TESTS=${OPTIMIZED_PACKAGE_TESTS:-0}
EOF
