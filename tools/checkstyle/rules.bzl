checkstyle_suppressions = "//tools/checkstyle:checkstyle-suppressions.xml"
checkstyle_xpath_suppressions = "//tools/checkstyle:checkstyle-xpath-suppressions.xml"
checkstyle_xml = "//tools/config/src/main/resources:harness_checks.xml"
checkstyle_jar = "//tools/checkstyle:checkstyle_deploy.jar"

def checkstyle():
    module_name = native.package_name()

    native.genrule(
        name = "checkstyle",
        srcs = native.glob(["src/**/*"]),
        tags = ["manual", "no-ide", "analysis", "checkstyle"],
        outs = ["checkstyle.xml"],
        visibility = ["//visibility:public"],
        cmd = " ".join([
            "java -classpath $(location " + checkstyle_jar + ")",
            "-Dorg.checkstyle.google.suppressionfilter.config=$(location " + checkstyle_suppressions + ")",
            "-Dorg.checkstyle.google.suppressionxpathfilter.config=$(location " + checkstyle_xpath_suppressions + ")",
            "com.puppycrawl.tools.checkstyle.Main",
            "-c $(location " + checkstyle_xml + ")",
            "-f xml",
            "-x \"src/generated\"",
            "--",
            module_name,
            "> \"$@\"",
            "&& sed -Ei.bak 's|/private/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& sed -Ei.bak 's|/tmp/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& if grep -B 1 \"<error \" \"$@\"; then exit 1; fi",
        ]),
        tools = [
            checkstyle_xml,
            checkstyle_suppressions,
            checkstyle_xpath_suppressions,
            checkstyle_jar,
        ],
    )
