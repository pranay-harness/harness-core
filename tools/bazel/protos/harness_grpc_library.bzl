load("@rules_proto_grpc//java:defs.bzl", "java_grpc_compile")

def harness_grpc_library(**kwargs):
    # Compile protos
    name_pb = kwargs.get("name") + "_pb"
    java_grpc_compile(
        name = name_pb,
        **{k: v for (k, v) in kwargs.items() if k in ("deps", "verbose")}  # Forward args
    )

    java_sources = []
    java_deps = []

    if kwargs.get("java_sources") != None:
        java_sources = kwargs.get("java_sources")

    if kwargs.get("java_deps") != None:
        java_deps = kwargs.get("java_deps")

    native.java_library(
        name = kwargs.get("name"),
        srcs = [name_pb] + java_sources,
        deps = GRPC_DEPS + java_deps,
        runtime_deps = ["@io_grpc_grpc_java//netty"],
        exports = GRPC_DEPS + java_deps,
        visibility = kwargs.get("visibility"),
    )

GRPC_DEPS = [
    "@com_google_guava_guava//jar",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@javax_annotation_javax_annotation_api//jar",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//protobuf",
    "@io_grpc_grpc_java//stub",
]
