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

# Enable caching by default. Turn it off by exporting CACHE_TEST_RESULTS=no
# to generate full call-graph for Test Intelligence
if [[ -z "${CACHE_TEST_RESULTS}" ]]; then
  export CACHE_TEST_RESULTS=yes
fi

if [ "${RUN_BAZEL_TESTS}" == "true" ]; then
  bazel ${bazelrc} build ${GCP} ${BAZEL_ARGUMENTS} -- //... -//product/... -//commons/... \
  && bazel ${bazelrc} test --cache_test_results=${CACHE_TEST_RESULTS} --define=HARNESS_ARGS=${HARNESS_ARGS} --keep_going ${GCP} ${BAZEL_ARGUMENTS} -- \
  //... -//product/... -//commons/... -//200-functional-test/... -//190-deployment-functional-tests/...
  exit $?
fi

if [ "${RUN_CHECKS}" == "true" ]; then
  TARGETS=$(bazel query 'attr(tags, "checkstyle", //...:*)')
  bazel ${bazelrc} build ${BAZEL_ARGUMENTS} -k ${TARGETS}
  exit $?
fi

if [ "${RUN_PMDS}" == "true" ]; then
  TARGETS=$(bazel query 'attr(tags, "pmd", //...:*)')
  bazel ${bazelrc} build ${BAZEL_ARGUMENTS} -k ${TARGETS}
  exit $?
fi

BAZEL_MODULES="\
  //990-commons-test:module \
"

bazel ${bazelrc} build `bazel query "//...:*" | grep "module_deploy.jar"` ${BAZEL_ARGUMENTS} --remote_download_outputs=all

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

  bazel ${bazelrc} build $BAZEL_MODULES ${BAZEL_ARGUMENTS}

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
    bazel ${bazelrc} build $BAZEL_DEPLOY_MODULE ${BAZEL_ARGUMENTS}
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

build_bazel_module 990-commons-test
