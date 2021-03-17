def aeriform(target):
    name = target.replace("/", "").replace(":", "!")
    native.genquery(
        testonly = True,
        name = name + "_aeriform_sources.txt",
        expression = "labels(srcs, " + target + ")",
        scope = [target],
        tags = ["manual", "no-ide", "aeriform"],
    )

    native.genquery(
        testonly = True,
        name = name + "_aeriform_dependencies.txt",
        expression = "labels(deps, " + target + ")",
        scope = [target],
        tags = ["manual", "no-ide", "aeriform"],
    )

    native.genrule(
        testonly = True,
        name = name + "_aeriform_jdeps",
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
