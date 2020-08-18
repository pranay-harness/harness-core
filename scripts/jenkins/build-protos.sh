#!/usr/bin/env bash

set -e

if [ ! -z "${BAZEL_OUTPUT_BASE}" ]
then
  OUTPUT_BASE="--output_base=/tmp"
fi

bazel ${OUTPUT_BASE} build -s \
  //22-ng-delegate-service-beans/src/main/proto/... \
  //24-manager-delegate-service-beans/src/main/proto/... \
  //24-ng-manager-service-beans/src/main/proto/... \
  //product/ci/engine/proto/... \
  --javacopt=' -XepDisableAllChecks'

cleanup() {
  for path in "$@"../protobuf/build-protos.sh
  do
    rm -rf ${path}
  done
}

compile_proto_module() {
  module=$1
  modulePath=$2
  generatedModulePath=$3
  bazel_library=`echo ${module} | tr '-' '_'`_proto

  mkdir -p ${generatedModulePath}
  if [ -e bazel-bin/${modulePath}/proto/${bazel_library}/java_grpc_compile_aspect_verb0 ]
  then
    unzip bazel-bin/${modulePath}/proto/${bazel_library}/java_grpc_compile_aspect_verb0/${bazel_library}.jar -d ${generatedModulePath}
    rm -rf ${generatedModulePath}/META-INF
    unzip bazel-bin/${modulePath}/proto/${bazel_library}/java_grpc_compile_aspect_verb0/${bazel_library}_grpc.jar -d ${generatedModulePath}
    rm -rf ${generatedModulePath}/META-INF
  else
    unzip bazel-bin/${modulePath}/proto/${bazel_library}/java_proto_compile_aspect_verb0/${bazel_library}.jar -d ${generatedModulePath}
    rm -rf ${generatedModulePath}/META-INF
  fi
  find ${generatedModulePath} -name '*.pb.meta' -delete
}

cleanup 22-ng-delegate-service-beans/src/generated/java \
        24-manager-delegate-service-beans/src/generated/java \
        24-ng-manager-service-beans/src/generated/java \
        72-ci-beans/src/generated/java

compile_proto_module 22-ng-delegate-service-beans 22-ng-delegate-service-beans/src/main 22-ng-delegate-service-beans/src/generated/java
compile_proto_module 24-manager-delegate-service-beans 24-manager-delegate-service-beans/src/main 24-manager-delegate-service-beans/src/generated/java
compile_proto_module 24-ng-manager-service-beans 24-ng-manager-service-beans/src/main 24-ng-manager-service-beans/src/generated/java

compile_proto_module cienginepb product/ci/engine 72-ci-beans/src/generated/java
compile_proto_module ciaddonpb product/ci/addon 72-ci-beans/src/generated/java

rm -f bazel-bin bazel-out bazel-portal bazel-testlogs