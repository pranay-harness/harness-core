#!/usr/bin/env bash

set -ex

ps auxwwwe
echo end off ps-report

local_repo=${HOME}/.m2/repository
BAZEL_ARGUMENTS=
if [ "${PLATFORM}" == "jenkins" ]; then
  GCP="--google_credentials=${GCP_KEY}"
  bazelrc=--bazelrc=bazelrc.remote
  local_repo=/root/.m2/repository
  if [ ! -z "${DISTRIBUTE_TESTING_WORKER}" ]; then
    bash scripts/bazel/testDistribute.sh
  fi
fi

BAZEL_DIRS=${HOME}/.bazel-dirs
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --experimental_convenience_symlinks=normal --symlink_prefix=${BAZEL_DIRS}/"
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --spawn_strategy=standalone"
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --test_timeout=900"

if [[ ! -z "${OVERRIDE_LOCAL_M2}" ]]; then
  local_repo=${OVERRIDE_LOCAL_M2}
fi

if [ "${STEP}" == "dockerization" ]; then
  GCP=""
fi
if [ "${RUN_BAZEL_FUNCTIONAL_TESTS}" == "true" ]; then

  bazel ${bazelrc} run ${GCP} ${BAZEL_ARGUMENTS} 230-model-test:app &
  java -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:mygclogfilename.gc -XX:+UseParallelGC -XX:MaxGCPauseMillis=500 -jar /root/.m2/repository/software/wings/260-delegate/0.0.1-SNAPSHOT/260-delegate-0.0.1-SNAPSHOT-capsule.jar /home/jenkins/workspace/pr-portal-funtional-tests/260-delegate/config-delegate.yml &
  sleep 300
  echo "Starting functional tests"
  bazel ${bazelrc} test --keep_going ${GCP} ${BAZEL_ARGUMENTS} --jobs=6 -- //200-functional-test/... //190-deployment-functional-tests:software.wings.functional.terraform.TerraformFunctionalTest //190-deployment-functional-tests:software.wings.functional.customDeployment.CustomDeploymentFunctionalTest || true
fi

ps auxwwwe
echo end off ps-report
