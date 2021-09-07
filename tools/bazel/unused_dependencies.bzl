def report_unused(rule_fn, **kwargs):
    name = kwargs.pop("name")
    deps = kwargs.pop("deps", [])
    data = kwargs.pop("data", [])
    kwargs.pop("testonly", None)

    i = 0
    for dep in deps:
        i += 1

        test_name = "unused/" + name + "_" + dep.replace("/", "!").replace(":", "~")
        prompt_name = test_name + "_prompt"
        report_name = test_name + "_report"

        native.genrule(
            name = prompt_name,
            outs = ["." + prompt_name],
            testonly = True,
            cmd = " && ".join([
                "echo \"# analizing %s:%s dependency: %s\"" % (native.package_name(), name, dep),
                "touch \"$@\"",
            ]),
        )

        rule_fn(
            name = test_name,
            deps = deps[:i - 1] + deps[i:],
            testonly = True,
            data = data + [prompt_name],
            **kwargs
        )

        native.genrule(
            name = report_name,
            outs = ["." + report_name],
            srcs = [test_name, prompt_name],
            testonly = True,
            cmd = " && ".join([
                "echo \"buildozer 'remove deps %s' //%s:%s\"" % (dep, native.package_name(), name),
                "touch \"$@\"",
            ]),
            tags = ["unused_dependency"],
        )
