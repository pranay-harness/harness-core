load("//tools/bazel/sonarqube:defs.bzl", "sq_project")

def resources(name = "resources", runtime_deps = [], testonly = 0, visibility = None):
    native.java_library(
        name = name,
        resources = native.glob(["**"], exclude = ["BUILD"]),
        resource_strip_prefix = "%s/" % native.package_name(),
        runtime_deps = runtime_deps,
        testonly = testonly,
        visibility = visibility,
    )

def sources(visibility = None):
    if visibility == None:
        visibility = ["//" + native.package_name() + ":__pkg__", "//:__pkg__"]
    native.filegroup(
        name = "sources",
        srcs = native.glob(["src/main/**/*.java"]),
        visibility = visibility,
    )

def test_targets_list(exclude = []):
    test_files = native.glob(["src/test/java/**/*Test.java"], exclude = exclude)
    return [file.split("/")[-1][:-5] for file in test_files]

def getCheckstyleReportPathForSonar():
    return "../../../" + native.package_name() + "/checkstyle.xml"

def sonarqube_test(
        name,
        project_key = None,
        project_name = None,
        srcs = [],
        source_encoding = None,
        targets = [],
        test_srcs = [],
        test_targets = [],
        test_reports = [],
        modules = {},
        sq_properties_template = None,
        tags = [],
        visibility = []):
    if project_key == None:
        project_key = native.package_name()
    if project_name == None:
        project_name = "Portal :: " + native.package_name()
    if test_srcs == []:
        test_srcs = native.glob(["src/test/**/*.java"])
    if test_targets == []:
        test_targets = test_targets_list()
    if test_reports == []:
        test_reports = ["//:test_reports"]
    if tags == []:
        tags = ["manual", "no-ide"]
    if visibility == []:
        visibility = ["//visibility:public"]
    sq_project(
        name = name,
        project_key = project_key,
        project_name = project_name,
        srcs = srcs,
        targets = targets,
        test_srcs = test_srcs,
        test_targets = test_targets,
        test_reports = test_reports,
        tags = tags,
        visibility = visibility,
        checkstyle_report_path = getCheckstyleReportPathForSonar(),
    )
