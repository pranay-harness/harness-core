def resources(name = "resources", runtime_deps=[], testonly = 0, visibility=None):
    native.java_library(
        name = name,
        resources = native.glob(["**"],exclude=["BUILD"]),
        resource_strip_prefix = "%s/" % native.package_name(),
        runtime_deps = runtime_deps,
        testonly = testonly,
        visibility = visibility
    )

def sources(visibility = None):
    if visibility == None:
      visibility = ["//"+native.package_name()+":__pkg__"]
    native.filegroup(
       name = "sources",
       srcs = native.glob(["src/main/**/*.java"]),
       visibility = visibility,
    )