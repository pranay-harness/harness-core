load("@rules_java//java:defs.bzl", "java_library")

gbq_tool = "@com_github_query_builder_generator//src:qbg"

def qbg_java(name, deps):
    qbg_files = native.glob(["**/*.qbg"])

    srcs = []
    for idx in range(len(qbg_files)):
        file = qbg_files[idx]
        qbg_file = qbg_files[idx][:-4]

        target = "java_gbg_" + file
        native.genrule(
            name = target,
            outs = [qbg_file + ".java"],
            srcs = [file],
            tools = [gbq_tool],
            tags = ["manual", "no-ide"],
            cmd = "$(location %s) generate --input $(location %s) --output \"$@\"" % (gbq_tool, file),
        )

        srcs += [target]

    java_library(
        name = name,
        srcs = srcs,
        deps = deps,
        visibility = ["//visibility:public"],
    )
