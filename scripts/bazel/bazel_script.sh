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
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... -//400-rest/... -//260-delegate/... -//900-yaml-sdk/...
  bazel ${bazelrc} test --keep_going ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... -//400-rest/... -//260-delegate/... -//900-yaml-sdk/...|| true
  # 400-rest and 260-delegate modules are excluded.
fi

BAZEL_MODULES="\
  //420-delegate-agent:module \
  //420-delegate-service:module \
  //430-cv-nextgen-commons:module \
  //440-connector-nextgen:module \
  //450-ce-views:module \
  //460-capability:module \
  //490-ce-commons:module \
  //780-ng-triggers:module \
  //800-pipeline-service:module \
  //800-pipeline-service:module_deploy.jar \
  //850-execution-plan:module \
  //850-ng-pipeline-commons:module \
  //860-orchestration-steps:module \
  //860-orchestration-visualization:module \
  //870-orchestration:module \
  //870-yaml-beans:module \
  //876-orchestration-beans:module \
  //879-pms-sdk:module \
  //882-pms-sdk-core:module \
  //884-pms-commons:module \
  //890-pms-contracts/src/main/proto:all \
  //890-pms-contracts:module \
  //890-sm-core:module \
  //910-delegate-service-driver:module \
  //910-delegate-task-grpc-service/src/main/proto:all \
  //910-delegate-task-grpc-service:module \
  //920-delegate-agent-beans/src/main/proto:all \
  //920-delegate-agent-beans:module \
  //920-delegate-service-beans/src/main/proto:all \
  //920-delegate-service-beans:module \
  //930-delegate-tasks:module \
  //930-ng-core-clients:module \
  //930-notification-service:module \
  //930-notification-service:module_deploy.jar \
  //940-delegate-beans/src/main/proto:all \
  //940-delegate-beans:module \
  //940-notification-client:module \
  //940-notification-client:module_deploy.jar \
  //940-secret-manager-client:module \
  //950-command-library-common:module \
  //950-common-entities:module \
  //950-delegate-tasks-beans/src/main/proto:all \
  //950-delegate-tasks-beans:module \
  //950-events-api/src/main/proto:all \
  //950-events-api:module \
  //950-events-framework:module \
  //950-feature-flag:module \
  //950-ng-core:module \
  //950-ng-project-n-orgs:module \
  //950-timeout-engine:module \
  //950-wait-engine:module \
  //950-walktree-visitor:module \
  //960-api-services:module \
  //960-expression-service/src/main/proto/io/harness/expression/service:all \
  //960-expression-service:module \
  //960-ng-core-beans:module \
  //960-notification-beans/src/main/proto:all \
  //960-notification-beans:module \
  //960-persistence:module \
  //960-persistence:supporter-test \
  //970-api-services-beans:module \
  //970-grpc:module \
  //970-ng-commons:module \
  //970-rbac-core:module \
  //980-commons:module \
  //990-commons-test:module \
  //product/ci/engine/proto:all \
  //product/ci/scm/proto:all \
"

bazel ${bazelrc} build $BAZEL_MODULES ${GCP} ${BAZEL_ARGUMENTS} --experimental_remote_download_outputs=all

build_bazel_module() {
  module=$1
  BAZEL_MODULE="//${module}:module"

  if ! grep -q "$BAZEL_MODULE" <<< "$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

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
  BAZEL_MODULE="//${module}:supporter-test"

  if ! grep -q "$BAZEL_MODULE" <<< "$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

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
  BAZEL_MODULE="//${module}:module"
  BAZEL_DEPLOY_MODULE="//${module}:module_deploy.jar"

  if ! grep -q "$BAZEL_MODULE" <<< "$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  if ! grep -q "$BAZEL_DEPLOY_MODULE" <<< "$BAZEL_MODULES"; then
    echo "$BAZEL_DEPLOY_MODULE is not in the list of modules"
    exit 1
  fi

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

  BAZEL_MODULE="//${modulePath}:all"

  if ! grep -q "$BAZEL_MODULE" <<< "$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

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

build_bazel_application 800-pipeline-service
build_bazel_application 930-notification-service
build_bazel_application 940-notification-client

build_bazel_module 420-delegate-agent
build_bazel_module 420-delegate-service
build_bazel_module 430-cv-nextgen-commons
build_bazel_module 440-connector-nextgen
build_bazel_module 450-ce-views
build_bazel_module 460-capability
build_bazel_module 490-ce-commons
build_bazel_module 780-ng-triggers
build_bazel_module 850-execution-plan
build_bazel_module 850-ng-pipeline-commons
build_bazel_module 860-orchestration-steps
build_bazel_module 860-orchestration-visualization
build_bazel_module 870-orchestration
build_bazel_module 870-yaml-beans
build_bazel_module 876-orchestration-beans
build_bazel_module 879-pms-sdk
build_bazel_module 882-pms-sdk-core
build_bazel_module 884-pms-commons
build_bazel_module 890-pms-contracts
build_bazel_module 890-sm-core
#build_bazel_module 900-yaml-sdk
build_bazel_module 910-delegate-service-driver
build_bazel_module 910-delegate-task-grpc-service
build_bazel_module 920-delegate-agent-beans
build_bazel_module 920-delegate-service-beans
build_bazel_module 930-delegate-tasks
build_bazel_module 930-ng-core-clients
build_bazel_module 940-delegate-beans
build_bazel_module 940-secret-manager-client
build_bazel_module 950-command-library-common
build_bazel_module 950-common-entities
build_bazel_module 950-delegate-tasks-beans
build_bazel_module 950-events-api
build_bazel_module 950-events-framework
build_bazel_module 950-feature-flag
build_bazel_module 950-ng-core
build_bazel_module 950-ng-project-n-orgs
build_bazel_module 950-timeout-engine
build_bazel_module 950-wait-engine
build_bazel_module 950-walktree-visitor
build_bazel_module 960-api-services
build_bazel_module 960-expression-service
build_bazel_module 960-ng-core-beans
build_bazel_module 960-notification-beans
build_bazel_module 960-persistence
build_bazel_module 970-api-services-beans
build_bazel_module 970-grpc
build_bazel_module 970-ng-commons
build_bazel_module 970-rbac-core
build_bazel_module 980-commons
build_bazel_module 990-commons-test

build_bazel_tests 960-persistence

build_java_proto_module 890-pms-contracts
build_java_proto_module 910-delegate-task-grpc-service proto
build_java_proto_module 920-delegate-agent-beans
build_java_proto_module 920-delegate-service-beans
build_java_proto_module 940-delegate-beans
build_java_proto_module 950-delegate-tasks-beans
build_java_proto_module 950-events-api
build_java_proto_module 960-notification-beans

build_proto_module 960-expression-service 960-expression-service/src/main/proto/io/harness/expression/service
build_proto_module ciengine product/ci/engine/proto
build_proto_module ciscm product/ci/scm/proto
