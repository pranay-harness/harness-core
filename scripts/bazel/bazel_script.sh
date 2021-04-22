#!/usr/bin/env bash

set -ex

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

if [[ ! -z "${OVERRIDE_LOCAL_M2}" ]]; then
  local_repo=${OVERRIDE_LOCAL_M2}
fi

if [ "${STEP}" == "dockerization" ]; then
  GCP=""
fi

# Enable caching by default. Turn it off by exporting CACHE_TEST_RESULTS=no
# to generate full call-graph for Test Intelligence
if [[ -z "${CACHE_TEST_RESULTS}" ]]; then
  export CACHE_TEST_RESULTS=yes
fi

if [ "${RUN_BAZEL_TESTS}" == "true" ]; then
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/...
  bazel ${bazelrc} test --cache_test_results=${CACHE_TEST_RESULTS} --define=HARNESS_ARGS=${HARNESS_ARGS} --keep_going ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... -//200-functional-test/... -//190-deployment-functional-tests/... || true
  exit 0
fi

if [ "${RUN_CHECKS}" == "true" ]; then
  TARGETS=$(bazel query 'attr(tags, "checkstyle", //...:*)')
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -k ${TARGETS}
  exit 0
fi

if [ "${RUN_PMDS}" == "true" ]; then
  TARGETS=$(bazel query 'attr(tags, "pmd", //...:*)')
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -k ${TARGETS}
  exit 0
fi

BAZEL_MODULES="\
  //110-change-data-capture:module \
  //120-ng-manager:module \
  //125-cd-nextgen:module \
  //160-model-gen-tool:module \
  //136-git-sync-manager:module \
  //200-functional-test:module \
  //210-command-library-xxxxxxxxmodule \
  //220-graphql-test:supporter-test \
  //230-model-test:module \
  //250-watcher:module \
  //260-delegate:module \
  //260-delegate:module_deploy.jar \
  //270-verification:module \
  //280-batch-processing:module \
  //300-cv-nextgen:module \
  //310-ci-manager:module \
  //320-ci-execution:module \
  //330-ci-beans:module \
  //340-ce-nextgen:module \
  //350-event-xxxxxxxxmodule \
  //360-cg-manager:module \
  //380-cg-graphql:module \
  //400-rest:module \
  //410-cg-rest:module \
  //400-rest:supporter-test \
  //420-delegate-agent:module \
  //420-delegate-service:module \
  //430-cv-nextgen-commons:module \
  //440-connector-nextgen:module \
  //445-cg-connectors:module \
  //450-ce-views:module \
  //460-capability:module \
  //490-ce-commons:module \
  //800-pipeline-service:module \
  //810-ng-triggers:module \
  //815-cg-triggers:module \
  //820-platform-service:module \
  //830-notification-service:module \
  //830-resource-group:module \
  //835-notification-senders:module \
  //850-execution-plan:module \
  //850-ng-pipeline-commons:module \
  //860-orchestration-steps:module \
  //860-orchestration-visualization:module \
  //870-cg-yaml-beans:module \
  //870-cg-orchestration:module \
  //870-orchestration:module \
  //870-yaml-beans:module \
  //871-cg-beans:module \
  //874-orchestration-delay:module \
  //876-orchestration-beans:module \
  //878-pipeline-service-utilities:module \
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
  //920-ng-signup:module \
  //925-access-control-service:module \
  //930-delegate-tasks:module \
  //930-ng-core-clients:module \
  //955-delegate-beans/src/main/proto:all \
  //955-delegate-beans:module \
  //935-access-control-decision:module \
  //940-feature-flag:module \
  //940-ng-audit-service:module \
  //940-notification-client:module \
  //940-resource-group-beans:module \
  //940-secret-manager-client:module \
  //945-ng-audit-client:module \
  //946-access-control-aggregator:module \
  //947-access-control-core:module \
  //948-access-control-admin-client:module \
  //948-access-control-sdk:module \
  //949-access-control-commons:module \
  //949-git-sync-sdk:module \
  //950-command-library-common:module \
  //950-cg-ng-shared-orchestration-beans:module \
  //950-common-entities:module \
  //950-delegate-tasks-beans/src/main/proto:all \
  //950-delegate-tasks-beans:module \
  //953-events-api/src/main/proto:all \
  //953-events-api:module \
  //950-events-framework:module \
  //950-ng-authentication-service:module \
  //950-ng-core:module \
  //950-ng-project-n-orgs:module \
  //950-log-client:module \
  //950-telemetry:module \
  //950-timeout-engine:module \
  //950-wait-engine:module \
  //950-walktree-visitor:module \
  //950-ng-audit-commons:module \
  //953-git-sync-commons:module \
  //953-git-sync-commons/src/main/proto:all \
  //954-connector-beans:module \
  //955-account-mgmt:module \
  //955-filters-sdk:module \
  //955-outbox-sdk:module \
  //955-setup-usage-sdk:module \
  //952-scm-java-client:module \
  //958-migration-sdk:module \
  //959-psql-database-models:module \
  //960-api-services:module \
  //960-continuous-features:module \
  //960-expression-service/src/main/proto/io/harness/expression/service:all \
  //960-expression-service:module \
  //960-ng-core-beans:module \
  //960-notification-beans/src/main/proto:all \
  //960-notification-beans:module \
  //960-persistence:module \
  //960-persistence:supporter-test \
  //960-recaster:module \
  //960-yaml-sdk:module \
  //970-api-services-beans/src/main/proto/io/harness/logging:all \
  //970-api-services-beans:module \
  //970-grpc:module \
  //970-ng-commons:module \
  //970-telemetry-beans:module \
  //970-rbac-core:module \
  //980-commons:module \
  //990-commons-test:module \
  //product/ci/engine/proto:all \
  //product/ci/scm/proto:all \
"

bazel ${bazelrc} build $BAZEL_MODULES ${GCP} ${BAZEL_ARGUMENTS} --remote_download_outputs=all

if [ "${RUN_BAZEL_FUNCTIONAL_TESTS}" == "true" ]; then
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} 360-cg-manager:module_deploy.jar
  bazel ${bazelrc} run ${GCP} ${BAZEL_ARGUMENTS} 230-model-test:app &
fi

build_bazel_module() {
  module=$1
  BAZEL_MODULE="//${module}:module"

  if ! grep -q "$BAZEL_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${module}/libmodule.jar"; then
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

  if ! grep -q "$BAZEL_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT-tests.jar" "${BAZEL_DIRS}/bin/${module}/libsupporter-test.jar"; then
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

  bazel ${bazelrc} build $BAZEL_MODULES ${GCP} ${BAZEL_ARGUMENTS}

  if ! grep -q "$BAZEL_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  if ! grep -q "$BAZEL_DEPLOY_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_DEPLOY_MODULE is not in the list of modules"
    exit 1
  fi

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${module}/module.jar"; then
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

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT-capsule.jar" "${BAZEL_DIRS}/bin/${module}/module_deploy.jar"; then
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

build_bazel_application_module() {
  module=$1
  BAZEL_MODULE="//${module}:module"
  BAZEL_DEPLOY_MODULE="//${module}:module_deploy.jar"

  if [ "${BUILD_BAZEL_DEPLOY_JAR}" == "true" ]; then
    bazel ${bazelrc} build $BAZEL_DEPLOY_MODULE ${GCP} ${BAZEL_ARGUMENTS}
  fi

  if ! grep -q "$BAZEL_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  if ! cmp -s "${local_repo}/software/wings/${module}/0.0.1-SNAPSHOT/${module}-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${module}/module.jar"; then
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

  if ! grep -q "$BAZEL_MODULE" <<<"$BAZEL_MODULES"; then
    echo "$BAZEL_MODULE is not in the list of modules"
    exit 1
  fi

  bazel_library=$(echo ${module} | tr '-' '_')

  if ! cmp -s "${local_repo}/software/wings/${module}-proto/0.0.1-SNAPSHOT/${module}-proto-0.0.1-SNAPSHOT.jar" "${BAZEL_DIRS}/bin/${modulePath}/lib${bazel_library}_java_proto.jar"; then
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

#if [ "${PLATFORM}" == "jenkins" ]
#then
#  build_bazel_module 990-commons-test
#  exit 0
#fi

build_bazel_application 260-delegate

build_bazel_application_module 110-change-data-capture
build_bazel_application_module 120-ng-manager
build_bazel_application_module 160-model-gen-tool
build_bazel_application_module 210-command-library-server
build_bazel_application_module 250-watcher
build_bazel_application_module 270-verification
build_bazel_application_module 280-batch-processing
build_bazel_application_module 300-cv-nextgen
build_bazel_application_module 310-ci-manager
build_bazel_application_module 340-ce-nextgen
build_bazel_application_module 350-event-server
build_bazel_application_module 360-cg-manager
build_bazel_application_module 800-pipeline-service
build_bazel_application_module 820-platform-service
build_bazel_application_module 925-access-control-service
build_bazel_application_module 940-notification-client

build_bazel_module 125-cd-nextgen
build_bazel_module 136-git-sync-manager
build_bazel_module 320-ci-execution
build_bazel_module 330-ci-beans
build_bazel_module 380-cg-graphql
build_bazel_module 400-rest
build_bazel_module 410-cg-rest
build_bazel_module 420-delegate-agent
build_bazel_module 420-delegate-service
build_bazel_module 430-cv-nextgen-commons
build_bazel_module 440-connector-nextgen
build_bazel_module 445-cg-connectors
build_bazel_module 450-ce-views
build_bazel_module 460-capability
build_bazel_module 490-ce-commons
build_bazel_module 810-ng-triggers
build_bazel_module 815-cg-triggers
build_bazel_module 830-notification-service
build_bazel_module 830-resource-group
build_bazel_module 835-notification-senders
build_bazel_module 850-execution-plan
build_bazel_module 850-ng-pipeline-commons
build_bazel_module 860-orchestration-steps
build_bazel_module 860-orchestration-visualization
build_bazel_module 870-cg-yaml-beans
build_bazel_module 870-cg-orchestration
build_bazel_module 870-orchestration
build_bazel_module 870-yaml-beans
build_bazel_module 871-cg-beans
build_bazel_module 874-orchestration-delay
build_bazel_module 876-orchestration-beans
build_bazel_module 878-pipeline-service-utilities
build_bazel_module 879-pms-sdk
build_bazel_module 882-pms-sdk-core
build_bazel_module 884-pms-commons
build_bazel_module 890-pms-contracts
build_bazel_module 890-sm-core
build_bazel_module 910-delegate-service-driver
build_bazel_module 910-delegate-task-grpc-service
build_bazel_module 920-delegate-agent-beans
build_bazel_module 920-delegate-service-beans
build_bazel_module 930-delegate-tasks
build_bazel_module 930-ng-core-clients
build_bazel_module 955-delegate-beans
build_bazel_module 935-access-control-decision
build_bazel_module 940-feature-flag
build_bazel_module 940-ng-audit-service
build_bazel_module 940-resource-group-beans
build_bazel_module 940-secret-manager-client
build_bazel_module 945-ng-audit-client
build_bazel_module 946-access-control-aggregator
build_bazel_module 947-access-control-core
build_bazel_module 948-access-control-admin-client
build_bazel_module 948-access-control-sdk
build_bazel_module 949-access-control-commons
build_bazel_module 950-command-library-common
build_bazel_module 950-cg-ng-shared-orchestration-beans
build_bazel_module 950-common-entities
build_bazel_module 950-delegate-tasks-beans
build_bazel_module 953-events-api
build_bazel_module 950-events-framework
build_bazel_module 950-ng-audit-commons
build_bazel_module 949-git-sync-sdk
build_bazel_module 950-log-client
build_bazel_module 950-ng-core
build_bazel_module 950-ng-project-n-orgs
build_bazel_module 950-timeout-engine
build_bazel_module 950-wait-engine
build_bazel_module 950-walktree-visitor
build_bazel_module 954-connector-beans
build_bazel_module 955-filters-sdk
build_bazel_module 955-outbox-sdk
build_bazel_module 955-setup-usage-sdk
build_bazel_module 952-scm-java-client
build_bazel_module 958-migration-sdk
build_bazel_module 959-psql-database-models
build_bazel_module 960-api-services
build_bazel_module 960-continuous-features
build_bazel_module 960-expression-service
build_bazel_module 960-ng-core-beans
build_bazel_module 960-notification-beans
build_bazel_module 960-persistence
build_bazel_module 960-recaster
build_bazel_module 960-yaml-sdk
build_bazel_module 953-git-sync-commons
build_bazel_module 970-api-services-beans
build_bazel_module 970-grpc
build_bazel_module 970-ng-commons
build_bazel_module 970-rbac-core
build_bazel_module 980-commons
build_bazel_module 990-commons-test
build_bazel_module 230-model-test
build_bazel_module 200-functional-test

build_bazel_tests 960-persistence
build_bazel_tests 400-rest
build_bazel_tests 220-graphql-test

build_java_proto_module 960-notification-beans

build_proto_module ciengine product/ci/engine/proto
build_proto_module ciscm product/ci/scm/proto
