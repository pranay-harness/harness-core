def java_package(index, package, **kwargs):
    native.java_library(
        name = str(index) + "-" + package.replace(".", "_"),
        srcs = native.glob(["src/main/java/" + package.replace(".", "/") + "/**/*.java"]),
        visibility = ["//visibility:private"],
        **kwargs
    )
