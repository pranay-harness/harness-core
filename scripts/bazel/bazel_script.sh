set -e

set -x

bazelrc=.bazelrc.local
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

  mvn -B install:install-file \
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
  modulePath=$2
  bazel --bazelrc=${bazelrc} build //${module}/src/main/${modulePath}/... ${GCP} ${BAZEL_ARGUMENTS} --javacopt=' -XepDisableAllChecks'

  bazel_library=`echo ${module} | tr '-' '_'`

  mvn -B install:install-file \
   -Dfile=../../bazel-bin/${module}/src/main/${modulePath}/lib${bazel_library}_java_proto.jar \
   -DgroupId=software.wings \
   -DartifactId=${module}-proto \
   -Dversion=0.0.1-SNAPSHOT \
   -Dpackaging=jar \
   -DgeneratePom=true \
   -DlocalRepositoryPath=${local_repo} \
   -f scripts/bazel/proto_pom.xml
}

build_bazel_module 11-commons-test
build_proto_module 13-grpc-api proto
build_proto_module 16-expression-service proto/io/harness/expression/service
build_proto_module 19-delegate-tasks-beans proto
build_proto_module 20-delegate-beans proto
build_proto_module 21-delegate-agent-beans proto
build_proto_module 22-delegate-service-beans proto

rm -f bazel-bin bazel-out bazel-portal bazel-testlogs