#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


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
