load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

exports_files([
    "checkstyle-suppressions.xml",
    "checkstyle-xpath-suppressions.xml",
    "rules.bzl",
])

bzl_library(
    name = "checkstyle-rules",
    srcs = ["rules.bzl"],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "checkstyle",
    srcs = glob(["src/main/java/io/harness/**/*.java"]),
    main_class = "com.puppycrawl.tools.checkstyle.Main",
    deps = [
        "checkstyle_deps",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "checkstyle_deps",
    exports = [
        "@maven//:antlr_antlr",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:com_puppycrawl_tools_checkstyle",
        "@maven//:commons_beanutils_commons_beanutils",
        "@maven//:info_picocli_picocli",
        "@maven//:commons_collections_commons_collections",
        "@maven//:com_google_guava_guava",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_slf4j_slf4j_simple",
        "@maven//:org_slf4j_jcl_over_slf4j",
        "@maven//:com_github_sevntu_checkstyle_sevntu_checks",
    ],
    visibility = ["//visibility:public"],
)
