set -e

set -x

bazelrc=.bazelrc
local_repo=${HOME}/.m2/repository
BAZEL_ARGUMENTS=""

if [ "${PLATFORM}" == "jenkins" ]
then
  GCP="--google_credentials=${GCP_KEY}"
  bazelrc=.bazelrc.remote
  local_repo=/root/.m2/repository
  BAZEL_ARGUMENTS="--action_env=HARNESS_GENERATION_PASSPHRASE=${HARNESS_GENERATION_PASSPHRASE}"
else
  BAZEL_ARGUMENTS="--define=ABSOLUTE_JAVABASE=${JAVA_HOME}"
fi

if [[ ! -z "${OVERRIDE_LOCAL_M2}" ]]; then
  local_repo=${OVERRIDE_LOCAL_M2}
fi


build_bazel_module() {
  module=$1
  bazel --bazelrc=${bazelrc} build //${module}:module ${GCP} ${BAZEL_ARGUMENTS}

  if [ "${RUN_BAZEL_TESTS}" == "true" ]
  then
    bazel --bazelrc=${bazelrc} test //${module}/... ${GCP} ${BAZEL_ARGUMENTS}
  fi

  mvn install:install-file \
   -Dfile=bazel-bin/${module}/libmodule.jar \
   -DgroupId=software.wings \
   -DartifactId=${module} \
   -Dversion=0.0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true \
   -DpomFile=${module}/pom.xml \
   -DlocalRepositoryPath=${local_repo}
}

build_proto_module() {
  module=$1
  bazel --bazelrc=${bazelrc} build //${module}/src/main/proto/... ${GCP} ${BAZEL_ARGUMENTS} --javacopt=' -XepDisableAllChecks'

  bazel_library=`echo ${module} | tr '-' '_'`

  mvn install:install-file \
   -Dfile=../../bazel-bin/${module}/src/main/proto/lib${bazel_library}_java_proto.jar \
   -DgroupId=software.wings \
   -DartifactId=${module}-proto \
   -Dversion=0.0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true \
   -DlocalRepositoryPath=${local_repo} \
   -f scripts/bazel/proto_pom.xml
}

build_bazel_module 11-commons-test
build_proto_module 13-grpc-api
build_proto_module 19-delegate-tasks-beans
build_proto_module 20-delegate-beans
build_proto_module 21-delegate-agent-beans
build_proto_module 22-delegate-service-beans

rm -f bazel-bin bazel-out bazel-portal bazel-testlogs