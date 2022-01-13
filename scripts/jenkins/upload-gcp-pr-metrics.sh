#!/usr/bin/env bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

export START_TIME=`cat start_time.dat`
export CHECK=${JOB_NAME}
export PR=${ghprbPullId}
export BUILD=${BUILD_ID}
export COMMIT=${GIT_COMMIT}

export COMMIT_TIME=$(git log -1 HEAD --pretty=format:%ct)
export END_TIME=$(date +%s)

export CHECK_EXECUTION_TIME="$(($END_TIME-$START_TIME))"

export CHECK_TOTAL_TIME="$(($END_TIME-$COMMIT_TIME))"

. scripts/jenkins/upload-gcp-metrics.sh pr
