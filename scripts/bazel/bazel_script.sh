#!/usr/bin/env bash

set -ex

local_repo=${HOME}/.m2/repository
BAZEL_ARGUMENTS=
if [ "${PLATFORM}" == "jenkins" ]
then
  GCP="--google_credentials=${GCP_KEY}"
  bazelrc=--bazelrc=bazelrc.remote
  local_repo=/root/.m2/repository
  if [ ! -z "${DISTRIBUTE_TESTING_WORKER}" ]
  then
    bash scripts/bazel/testDistribute.sh
  fi
fi

BAZEL_DIRS=${HOME}/.bazel-dirs
BAZEL_ARGUMENTS="${BAZEL_ARGUMENTS} --experimental_convenience_symlinks=normal --symlink_prefix=${BAZEL_DIRS}/"

if [[ ! -z "${OVERRIDE_LOCAL_M2}" ]]; then
  local_repo=${OVERRIDE_LOCAL_M2}
fi

if [ "${STEP}" == "dockerization" ]
then
  GCP=""
fi

if [ "${RUN_BAZEL_TESTS}" == "true" ]
then
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... -//71-rest/... -//260-delegate/... -//460-capability/...
  bazel ${bazelrc} test --keep_going ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... -//71-rest/... -//260-delegate/... || true
  # 71-rest and 260-delegate modules are excluded.
fi

build_bazel_module() {
  module=$1
  bazel ${bazelrc} build //${module}:module ${GCP} ${BAZEL_ARGUMENTS} --experimental_remote_download_outputs=all

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${module}/libmodule.jar"
  then
    mvn -B install:install-file \
     -Dfile=${BAZEL_DIRS}/bin/${module}/libmodule.jar \
     -DgroupId=software.wings \
     -DartifactId=${module} \
     -Dversion=0.0.1-SNAPSHOT \
     -Dpackaging=jar \
     -DgeneratePom=true \
     -DpomFile=${module}/pom.xml \
     -DlocalRepositoryPath=${local_repo}
  fi
}

build_bazel_tests() {
  module=$1

  bazel ${bazelrc} build //${module}:supporter-test ${GCP} ${BAZEL_ARGUMENTS} --experimental_remote_download_outputs=all

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT-tests.jar" "${BAZEL_DIRS}/bin/${module}/libsupporter-test.jar"
  then
    mvn -B install:install-file \
     -Dfile=${BAZEL_DIRS}/bin/${module}/libsupporter-test.jar \
     -DgroupId=software.wings \
     -DartifactId=${module} \
     -Dversion=0.0.1-SNAPSHOT \
     -Dclassifier=tests \
     -Dpackaging=jar \
     -DgeneratePom=true \
     -DpomFile=${module}/pom.xml \
     -DlocalRepositoryPath=${local_repo}
  fi
}

build_bazel_application() {
  module=$1
  bazel ${bazelrc} build //${module}:module //${module}:module_deploy.jar ${GCP} ${BAZEL_ARGUMENTS}

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${module}/module.jar"
  then
    mvn -B install:install-file \
     -Dfile=${BAZEL_DIRS}/bin/${module}/module.jar \
     -DgroupId=software.wings \
     -DartifactId=${module} \
     -Dversion=0.0.1-SNAPSHOT \
     -Dpackaging=jar \
     -DgeneratePom=true \
     -DpomFile=${module}/pom.xml \
     -DlocalRepositoryPath=${local_repo}
  fi

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT-capsule.jar" "${BAZEL_DIRS}/bin/${module}/module_deploy.jar"
  then
    mvn -B install:install-file \
     -Dfile=${BAZEL_DIRS}/bin/${module}/module_deploy.jar \
     -DgroupId=software.wings \
     -DartifactId=${module} \
     -Dversion=0.0.1-SNAPSHOT \
     -Dclassifier=capsule \
     -Dpackaging=jar \
     -DgeneratePom=true \
     -DpomFile=${module}/pom.xml \
     -DlocalRepositoryPath=${local_repo}
  fi
}

build_java_proto_module() {
  module=$1
  modulePath=$module/src/main/proto

  build_proto_module $module $modulePath
}

build_proto_module() {
  module=$1
  modulePath=$2
  bazel ${bazelrc} build //${modulePath}:all ${GCP} ${BAZEL_ARGUMENTS} --experimental_remote_download_outputs=all

  bazel_library=`echo ${module} | tr '-' '_'`

  if ! cmp -s "${local_repo}/software/wings/${module}-proto/0.0.1-SNAPSHOT/${module}-proto-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${modulePath}/lib${bazel_library}_java_proto.jar"
  then
    mvn -B install:install-file \
     -Dfile=${BAZEL_DIRS}/bin/${modulePath}/lib${bazel_library}_java_proto.jar \
     -DgroupId=software.wings \
     -DartifactId=${module}-proto \
     -Dversion=0.0.1-SNAPSHOT \
     -Dpackaging=jar \
     -DgeneratePom=true \
     -DlocalRepositoryPath=${local_repo} \
     -f scripts/bazel/proto_pom.xml
  fi
}

build_bazel_module 15-api-services
build_bazel_module 16-expression-service
build_bazel_module 19-delegate-tasks-beans
build_bazel_module 20-delegate-beans
build_bazel_module 20-delegate-tasks
build_bazel_module 20-ng-core-beans
build_bazel_module 20-notification-beans
build_bazel_module 21-delegate-agent-beans
build_bazel_application 21-events-api
build_bazel_application 21-notification-client
build_bazel_module 21-persistence
build_bazel_tests 21-persistence
build_bazel_module 22-delegate-service-beans
build_bazel_module 22-ng-core
build_bazel_module 22-ng-project-n-orgs
build_bazel_module 22-rbac-core
build_bazel_module 22-secret-manager-client
build_bazel_module 22-timeout-engine
build_bazel_module 22-wait-engine
build_bazel_module 23-ng-core-clients
build_bazel_module 23-delegate-service-driver
build_bazel_application 23-notification-service
build_bazel_module 23-sm-core
build_bazel_module 24-common-entities
build_bazel_module 26-pms-contracts
build_bazel_module 27-orchestration-persistence
build_bazel_module 28-pms-beans
build_bazel_module 29-orchestration-beans
build_bazel_module 31-orchestration
build_bazel_module 32-orchestration-steps
build_bazel_module 33-orchestration-visualization
build_bazel_module 34-walktree-visitor
build_bazel_module 35-yaml-beans
build_bazel_module 38-execution-plan
build_bazel_module 39-ng-pipeline-commons
build_bazel_module 40-ng-triggers
build_bazel_module 40-pms-commons
build_bazel_module 41-pms-sdk
build_bazel_module 50-delegate-task-grpc-service
build_bazel_module 57-command-library-common
build_bazel_module 64-events-framework
build_bazel_module 420-delegate-agent
build_bazel_module 420-delegate-service
build_bazel_module 430-cv-nextgen-commons
build_bazel_module 440-connector-nextgen
build_bazel_module 450-ce-views
build_bazel_module 490-ce-commons
build_bazel_module 900-yaml-sdk
build_bazel_module 970-api-services-beans
build_bazel_module 970-grpc
build_bazel_module 970-ng-commons
build_bazel_module 980-commons
build_bazel_module 990-commons-test

build_java_proto_module 19-delegate-tasks-beans
build_java_proto_module 20-delegate-beans
build_java_proto_module 21-events-api
build_java_proto_module 20-notification-beans
build_java_proto_module 21-delegate-agent-beans
build_java_proto_module 22-delegate-service-beans
build_java_proto_module 26-pms-contracts
build_java_proto_module 50-delegate-task-grpc-service proto

build_proto_module 16-expression-service 16-expression-service/src/main/proto/io/harness/expression/service
build_proto_module ciscm product/ci/scm/proto
build_proto_module ciengine product/ci/engine/proto
