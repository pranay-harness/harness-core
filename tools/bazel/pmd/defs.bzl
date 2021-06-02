rulesets = "//tools/config/src/main/resources:harness_pmd_ruleset.xml"
pmd_tool = "//tools/bazel/pmd:pmd"

def pmd():
    module_name = native.package_name()
    native.genrule(
        name = "pmd",
        outs = ["pmd_report.xml"],
        srcs = native.glob(["src/main/**"]),
        tags = ["manual", "no-ide", "analysis", "pmd"],
        visibility = ["//visibility:public"],
        cmd = " ".join([
            "$(location " + pmd_tool + ")",
            "-d " + module_name,
            "-f xml",
            "-failOnViolation False",
            "-R $(location " + rulesets + ")",
            "-language java",
            "-cache pmd_report.cache",
            "> \"$@\"",
            "&& sed -Ei.bak 's|/private/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& sed -Ei.bak 's|/tmp/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& if grep -B 1 \"<violation \" \"$@\"; then exit 1; fi",
        ]),
        tools = [
            pmd_tool,
            rulesets,
        ],
    )
