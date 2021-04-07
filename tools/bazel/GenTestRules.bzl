load("//:test-util.bzl", "DISTRIBUTE_TESTING_WORKER", "DISTRIBUTE_TESTING_WORKERS")

def run_tests(**kwargs):
    test_files = native.glob(["src/test/**/*Test.java"])
    for idx in range(len(test_files)):
        test = test_files[idx][14:][:-5].replace("/", ".")
        x = hash(test)
        if (x % DISTRIBUTE_TESTING_WORKERS != DISTRIBUTE_TESTING_WORKER):
            continue
        native.java_test(
            name = test,
            runtime_deps = ["tests"],
            size = "large",
            jvm_flags = [
                "$(HARNESS_ARGS)",
                "-Xmx1G",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=$${TEST_WARNINGS_OUTPUT_FILE}/../heap.hprof",
            ],
            test_class = test,
            testonly = True,
            visibility = ["//visibility:private"],
            **kwargs
        )

def run_package_tests(deps = [], data = [], resources = []):
    all_srcs = native.glob(["src/test/**/*.java"])

    directories = {}
    for src in all_srcs:
        directory = src[0:src.rfind("/")]
        srcs = directories.setdefault(directory, [])
        srcs.append(src)

    native.java_library(
        name = "shared_package_tests",
        testonly = True,
        srcs = native.glob(
            include = ["src/test/**/*.java"],
            exclude = ["src/test/**/*Test.java"],
        ),
        data = data,
        resources = resources,
        deps = deps,
    )

    for directory in directories.items():
        junit_package_test(directory[0], directory[1], deps)

template = """
package %s;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ %s })
public class AllTests%s {
}

"""

MAX_TESTS = 16

def calculate_index(length, i):
    if length < MAX_TESTS:
        return ""
    return str(i // MAX_TESTS + 1)

def junit_package_test(directory, srcs, deps):
    truncate = len(directory) + 1

    shared_src = []
    all_tests = {}
    for src in srcs:
        if src.endswith("Test.java"):
            all_tests[src] = src[truncate:].replace(".java", ".class")
        else:
            shared_src += [src]

    if len(all_tests) == 0:
        return

    package = directory.replace("src/test/java/", "").replace("/", ".")

    for i in range(0, len(all_tests), MAX_TESTS):
        tests = all_tests.items()[i:i + MAX_TESTS]

        index = calculate_index(len(all_tests), i)

        code = template % (package, ", \n                      ".join([x[1] for x in tests]), index)

        test_class = "AllTests" + index

        code_filepath = [directory + "/" + test_class + ".java"]

        if (hash(code_filepath[0]) % DISTRIBUTE_TESTING_WORKERS != DISTRIBUTE_TESTING_WORKER):
            continue

        native.genrule(
            name = package + ".AllTests" + index + "-gen",
            outs = code_filepath,
            cmd = """
cat <<EOF >> $@
%s
EOF""" % code,
        )

        native.java_test(
            name = package + ".tests" + index,
            test_class = package + "." + test_class,
            deps = [":shared_package_tests"] + deps,
            size = "enormous",

            # inputs
            srcs = code_filepath + [x[0] for x in tests],

            #Additional
            visibility = ["//visibility:public"],
            jvm_flags = [
                "$(HARNESS_ARGS)",
                "-Xmx2G",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=$${TEST_WARNINGS_OUTPUT_FILE}/../heap.hprof",
            ],
        )
