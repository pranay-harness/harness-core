def pmd(
        name = "pmd",
        rulesets = None,
        srcs = None,
        visibility = ["//visibility:public"]):
    if srcs == None:
        srcs = native.glob(["src/main/**"])
    if rulesets == None:
        rulesets = "//tools/config/src/main/resources:harness_pmd_ruleset.xml"

    _pmd(name = name, srcs = srcs, rulesets = rulesets, visibility = visibility)

def _pmd(
        srcs = [],
        language = "java",
        rulesets = "//tools/config/src/main/resources:harness_pmd_ruleset.xml",
        report_format = "xml",
        **kwargs):
    module_name = native.package_name()
    native.genrule(
        name = "pmd",
        outs = ["pmd_report.xml"],
        srcs = srcs,
        tags = ["manual", "no-ide", "analysis", "pmd"],
        visibility = ["//visibility:public"],
        cmd = " ".join([
            "$(location //tools/bazel/pmd:pmd)",
            "-d  " + module_name,
            "-f " + report_format,
            "-failOnViolation False",
            "-R $(location " + rulesets + ")",
            "-language " + language,
            "| tee \"$@\"",
        ]),
        tools = [
            "//tools/bazel/pmd:pmd",
            "//tools/config/src/main/resources:harness_pmd_ruleset.xml",
        ],
    )
