load("@rules_java//java:defs.bzl", "java_binary", "java_library")

def application_version(name):
    version_file_name = name + "_version_info_yaml"
    native.genrule(
        name = version_file_name,
        srcs = ["//:tools/bazel/version.sh"],
        outs = [name + "_version/io/harness/versionInfo.yaml"],
        cmd = "$(location //:tools/bazel/version.sh) \"bazel-out/stable-status.txt\" \"bazel-out/volatile-status.txt\" >>  \"$@\"",
        visibility = ["//visibility:public"],
        stamp = True,
    )

    native.java_library(
        name = name + "_version",
        resources = [":" + version_file_name],
        resource_strip_prefix = "%s/%s_version/" % (native.package_name(), name),
    )

def harness_java_library(**kwargs):
    #    name = kwargs.get("name")
    #    application_version(name)
    #    kwargs["runtime_deps"] = kwargs.pop("runtime_deps", []) + [":" + name + "_version"]
    java_library(**kwargs)

def harness_java_binary(**kwargs):
    #    name = kwargs.get("name")
    #    application_version(name)
    #    kwargs["runtime_deps"] = kwargs.pop("runtime_deps", []) + [":" + name + "_version"]
    java_binary(**kwargs)
