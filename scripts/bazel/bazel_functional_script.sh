#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


set -ex

local_repo=${HOME}/.m2/repository
BAZEL_ARGUMENTS=
BAZEL_TEST_ARGUMENTS=
if [ "${PLATFORM}" == "jenkins" ]; then
  GCP="--google_credentials=${GCP_KEY}"
  local_repo=/root/.m2/repository
  if [ ! -z "${DISTRIBUTE_TESTING_WORKER}" ]; then
    bash scripts/bazel/testDistribute.sh
  fi
fi

BAZEL_DIRS=${HOME}/.bazel-dirs
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --experimental_convenience_symlinks=normal --symlink_prefix=${BAZEL_DIRS}/"
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --spawn_strategy=standalone"
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --test_timeout=900"
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --test_output=all"
#BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --cache_test_results=no"
BAZEL_TEST_ARGUMENTS="${BAZEL_TEST_ARGUMENTS} --test_verbose_timeout_warnings --test_summary=detailed"

if [[ ! -z "${OVERRIDE_LOCAL_M2}" ]]; then
  local_repo=${OVERRIDE_LOCAL_M2}
fi

if [ "${RUN_BAZEL_FUNCTIONAL_TESTS}" == "true" ]; then

  bazel build ${GCP} ${BAZEL_ARGUMENTS} -- //200-functional-test/...
  curl https://storage.googleapis.com/harness-prod-public/public/shared/tools/alpn/release/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar --output alpn-boot-8.1.13.v20181017.jar
  java -Xbootclasspath/p:alpn-boot-8.1.13.v20181017.jar -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError \
    -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC \
    -XX:MaxGCPauseMillis=500 -jar /harness/bazel-out/k8-fastbuild/bin/260-delegate/module_deploy.jar /harness/260-delegate/config-delegate.yml &> /harness/delegate.log &
  DELEGATE_PID=$!

  bazel run ${GCP} ${BAZEL_ARGUMENTS} 230-model-test:app
#  MODEL_TEST_APP_PID=$!

#  sleep 5m;
  MANAGER_PID=`ps ax | grep 'java' | grep '360-cg-manager/modified_config' |  awk '{print $1}'`
  echo "INFO: MANAGER_PID = $MANAGER_PID"

  bazel test --keep_going ${GCP} ${BAZEL_ARGUMENTS} --jobs=3 ${BAZEL_TEST_ARGUMENTS} -- //200-functional-test:io.harness.functional.DummyFirstFunctionalTest || true

#  TODO: https://harness.atlassian.net/browse/BT-434
  bazel test --keep_going ${GCP} ${BAZEL_ARGUMENTS} --jobs=3 ${BAZEL_TEST_ARGUMENTS} -- //200-functional-test/... \
    -//200-functional-test:io.harness.functional.nas.NASBuildWorkflowExecutionTest \
    -//200-functional-test:io.harness.functional.nas.NASWorkflowExecutionTest || true

  echo "INFO: MANAGER_PID = $MANAGER_PID"
  echo "INFO: DELEGATE_PID = $DELEGATE_PID"
#  echo "INFO: MODEL_TEST_APP_PID = $MODEL_TEST_APP_PID"

  kill -9 $MANAGER_PID || true
  kill -9 $DELEGATE_PID || true
#  kill -9 $MODEL_TEST_APP_PID || true
#  pkill -P $$
fi
