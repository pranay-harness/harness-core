def impl_checkstyle(name, tags, srcs=[],
               checkstyle_suppressions="//tools/checkstyle:checkstyle-suppressions.xml",
               checkstyle_xpath_suppressions="//tools/checkstyle:checkstyle-xpath-suppressions.xml",
               checkstyle_xml="//tools/config/src/main/resources:harness_checks.xml"):

    native.genrule(
        name = name,
        srcs = srcs,
        tags = tags,
        outs = ["checkstyle.xml"],
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
            "&& sed -Ei.bak 's|sandbox/darwin-sandbox/[0-9]+/execroot|execroot/harness_monorepo/bazel-out/darwin-fastbuild/bin/sq.runfiles|g'  \"$@\"",
            "&& sed -Ei.bak 's|sandbox/[a-zA-Z]+-sandbox/[0-9]+/execroot|execroot/harness_monorepo/bazel-out/k8-fastbuild/bin/sq.runfiles|g' \"$@\"",
            "&& cat \"$@\""
        ]),
        tools = [
            checkstyle_xml,
            checkstyle_suppressions,
            checkstyle_xpath_suppressions,
            "//tools/checkstyle:checkstyle_deploy.jar",
        ],
    )

def checkstyle(name="checkstyle", srcs = None,tags = ["manual","no-ide"]):
    if srcs == None:
        srcs = ["//"+native.package_name()+":sources"]
    impl_checkstyle(name=name, srcs=srcs, tags=tags)