load("@rules_java//java:defs.bzl", "java_binary", "java_library")

def harness_java_library(**kwargs):
    java_library(**kwargs)

def harness_sign(jar):
    name = jar.rsplit("/", 1)[-1][:-4] + "_signed"
    signed_jar = jar.rsplit(":", 1)[-1][:-4] + "_signed.jar"

    native.genrule(
        name = name,
        srcs = [jar],
        outs = [signed_jar],
        exec_tools = ["//:tools/bazel/signer.sh"],
        cmd = " && ".join([
            "cp $(location %s) \"$@\"" % (jar),
            " ".join([
                "$(location //:tools/bazel/signer.sh)",
                "\"bazel-out/stable-status.txt\"",
                "$(JAVABASE)/bin/jarsigner",
                "-storetype pkcs12",
                "-keystore \\$${SIGNER_KEY_STORE}",
                "-storepass \\$${SIGNER_KEY_STORE_PASSWORD}",
                "\"$@\"",
                "harnessj",
            ]),
        ]),
        visibility = ["//visibility:public"],
        toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
        stamp = True,
    )

    return name

def harness_java_binary(**kwargs):
    name = kwargs.get("name")

    sign = kwargs.pop("sign")
    java_binary(**kwargs)

    if sign:
        harness_sign(name + "_deploy.jar")
