def aeriform(target):
    name = target.replace("/", "").replace(":", "!")
    native.genquery(
        testonly = True,
        name = name + "_aeriform_sources.txt",
        testonly = True,
        expression = "labels(srcs, " + target + ")",
        scope = [target],
        tags = ["manual", "no-ide", "aeriform"],
    )

    native.genquery(
        testonly = True,
        name = name + "_aeriform_dependencies.txt",
        testonly = True,
        expression = "labels(deps, " + target + ")",
        scope = [target],
        tags = ["manual", "no-ide", "aeriform"],
    )

    native.genrule(
        testonly = True,
        name = name + "_aeriform_jdeps",
        testonly = True,
        outs = [name + "_aeriform_jdeps.txt"],
        tags = ["manual", "no-ide", "aeriform"],
        srcs = [target],
        cmd = " ".join([
            "jdeps",
            "-v",
            "$(locations " + target + ")",
            "> \"$@\"",
        ]),
    )
