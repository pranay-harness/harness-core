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
                "-Xmx1G",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=$${TEST_WARNINGS_OUTPUT_FILE}/../heap.hprof",
            ],
            test_class = test,
            testonly = 1,
            visibility = ["//visibility:private"],
            **kwargs
        )
