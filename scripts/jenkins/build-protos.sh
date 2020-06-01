#!/usr/bin/env bash

set -e

if [ ! -z "${BAZEL_OUTPUT_BASE}" ]
then
  OUTPUT_BASE="--output_base=${BAZEL_OUTPUT_BASE}"
fi

bazel ${OUTPUT_BASE} build \
  //19-delegate-tasks-beans/src/... \
  //19-grpc-api/src/main/proto/... \
  //64-delegate-beans/src/main/proto/... \
  //65-delegate-agent-beans/src/main/proto/... \
  //65-delegate-service-beans/src/main/proto/...

compile_proto_module() {
  module=$1
  bazel_library=`echo ${module} | tr '-' '_'`_proto

  rm -rf ${module}/src/generated/java
  mkdir -p ${module}/src/generated/java
  if [ -e bazel-bin/${module}/src/main/proto/${bazel_library}/java_grpc_compile_aspect_verb0 ]
  then
    unzip bazel-bin/${module}/src/main/proto/${bazel_library}/java_grpc_compile_aspect_verb0/${bazel_library}.jar -d ${module}/src/generated/java
    rm -rf ${module}/src/generated/java/META-INF
    unzip bazel-bin/${module}/src/main/proto/${bazel_library}/java_grpc_compile_aspect_verb0/${bazel_library}_grpc.jar -d ${module}/src/generated/java
    rm -rf ${module}/src/generated/java/META-INF
  else
    unzip bazel-bin/${module}/src/main/proto/${bazel_library}/java_proto_compile_aspect_verb0/${bazel_library}.jar -d ${module}/src/generated/java
    rm -rf ${module}/src/generated/java/META-INF
  fi
  find ${module}/src/generated -name '*.pb.meta' -delete
}

compile_proto_module 19-delegate-tasks-beans
compile_proto_module 19-grpc-api

compile_proto_module 64-delegate-beans
compile_proto_module 65-delegate-agent-beans
compile_proto_module 65-delegate-service-beans

rm -f bazel-bin bazel-out bazel-portal bazel-testlogs
