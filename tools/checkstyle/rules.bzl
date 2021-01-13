def impl_checkstyle(
        name,
        tags,
        srcs = [],
        checkstyle_suppressions = "//tools/checkstyle:checkstyle-suppressions.xml",
        checkstyle_xpath_suppressions = "//tools/checkstyle:checkstyle-xpath-suppressions.xml",
        checkstyle_xml = "//tools/config/src/main/resources:harness_checks.xml"):
    native.genrule(
        name = name,
        srcs = srcs,
        tags = tags,
        outs = ["checkstyle.xml"],
        visibility = ["//visibility:public"],
        cmd = " ".join([
            "java -classpath $(location //tools/checkstyle:checkstyle_deploy.jar)",
            "-Dorg.checkstyle.google.suppressionfilter.config=$(location " + checkstyle_suppressions + ")",
            "-Dorg.checkstyle.google.suppressionxpathfilter.config=$(location " + checkstyle_xpath_suppressions + ")",
            "com.puppycrawl.tools.checkstyle.Main",
            "-c $(location " + checkstyle_xml + ")",
            "-f xml",
            "--",
            "$(SRCS)",
            "> \"$@\"",
            "&& sed -Ei.bak 's|/private/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& sed -Ei.bak 's|/tmp/(.*)/harness_monorepo/||g'  \"$@\"",
            "&& if grep -B 1 \"<error \" \"$@\"; then exit 1; fi",
        ]),
        tools = [
            checkstyle_xml,
            checkstyle_suppressions,
            checkstyle_xpath_suppressions,
            "//tools/checkstyle:checkstyle_deploy.jar",
        ],
    )

def checkstyle(name = "checkstyle", srcs = None, tags = ["manual", "no-ide"]):
    if srcs == None:
        srcs = ["//" + native.package_name() + ":sources"]
    impl_checkstyle(name = name, srcs = srcs, tags = tags)

def get_checkstyle_targets(modules = []):
    _targets = []
    for f in modules:
        _targets.append(f + ":checkstyle")
    return _targets
