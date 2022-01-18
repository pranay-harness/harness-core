load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def rules_pmd_dependencies():
    """Fetches `rules_pmd` dependencies.

    Declares dependencies of the `rules_pmd` workspace.
    Users should call this macro in their `WORKSPACE` file.
    """

    rules_java_version = "0.1.1"
    rules_java_sha = "220b87d8cfabd22d1c6d8e3cdb4249abd4c93dcc152e0667db061fb1b957ee68"

    maybe(
        repo_rule = http_archive,
        name = "rules_java",
        url = "http://jfrogdev.dev.harness.io:80/artifactory/rules-java-github/download/{v}/rules_java-{v}.tar.gz".format(v = rules_java_version),
        sha256 = rules_java_sha,
    )
