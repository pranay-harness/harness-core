load("//tools/bazel/sonarqube:defs.bzl", "sq_project")
load("//tools/checkstyle:rules.bzl", "checkstyle")
load("//tools/bazel/pmd:defs.bzl", "pmd")
load("@rules_jvm_external//:specs.bzl", "maven")

def resources(name = "resources", runtime_deps = [], testonly = 0, visibility = None):
    native.java_library(
        name = name,
        resources = native.glob(["**"], exclude = ["BUILD"]),
        resource_strip_prefix = "%s/" % native.package_name(),
        runtime_deps = runtime_deps,
        testonly = testonly,
        visibility = visibility,
    )

def test_targets_list(exclude = []):
    test_files = native.glob(["src/test/java/**/*Test.java"], exclude = exclude)
    return [file.split("/")[-1][:-5] for file in test_files]

def getCheckstyleReportPathForSonar():
    return "../../../" + native.package_name() + "/checkstyle.xml"

def sonarqube_test(
        name = None,
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
    srcs = native.glob(["src/main/**/*.java"])
    targets = [":module"]
    if name == None:
        name = "sq_mycomponent"
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

def run_analysis(
        run_checkstyle = True,
        run_pmd = True,
        run_sonar = True):
    if run_checkstyle:
        checkstyle()

    if run_pmd:
        pmd()

    if run_sonar:
        sonarqube_test()

def maven_test_artifact(artifact):
    entities = artifact.split(":")
    return maven.artifact(
        group = entities[0],
        artifact = entities[1],
        version = entities[2],
        testonly = True,
    )
