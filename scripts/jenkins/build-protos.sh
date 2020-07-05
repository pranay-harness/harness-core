#!/usr/bin/env bash

set -e

if [ ! -z "${BAZEL_OUTPUT_BASE}" ]
then
  OUTPUT_BASE="--output_base=${BAZEL_OUTPUT_BASE}"
fi

bazel ${OUTPUT_BASE} build \
  //13-grpc-api/src/main/proto/... \
  //19-delegate-tasks-beans/src/... \
  //20-delegate-beans/src/main/proto/... \
  //21-delegate-agent-beans/src/main/proto/... \
  //22-delegate-service-beans/src/main/proto/... \
  //24-ng-delegate-service-beans/src/main/proto/... \
  //product/ci/engine/proto/...

cleanup() {
  for path in "$@"
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

cleanup 13-grpc-api/src/generated/java \
        19-delegate-tasks-beans/src/generated/java \
        20-delegate-beans/src/generated/java \
        21-delegate-agent-beans/src/generated/java \
        22-delegate-service-beans/src/generated/java \
        24-ng-delegate-service-beans/src/generated/java \
        72-ci-beans/src/generated/java

compile_proto_module 13-grpc-api 13-grpc-api/src/main 13-grpc-api/src/generated/java

compile_proto_module 19-delegate-tasks-beans 19-delegate-tasks-beans/src/main 19-delegate-tasks-beans/src/generated/java
compile_proto_module 20-delegate-beans 20-delegate-beans/src/main 20-delegate-beans/src/generated/java
compile_proto_module 21-delegate-agent-beans 21-delegate-agent-beans/src/main 21-delegate-agent-beans/src/generated/java
compile_proto_module 22-delegate-service-beans 22-delegate-service-beans/src/main 22-delegate-service-beans/src/generated/java
compile_proto_module 24-ng-delegate-service-beans 24-ng-delegate-service-beans/src/main 24-ng-delegate-service-beans/src/generated/java

compile_proto_module cienginepb product/ci/engine 72-ci-beans/src/generated/java
compile_proto_module ciaddonpb product/ci/addon 72-ci-beans/src/generated/java

rm -f bazel-bin bazel-out bazel-portal bazel-testlogs