def run_tests(**kwargs):
    test_files = native.glob(["src/test/**/*Test.java"])
    for idx in range(len(test_files)):
        test = test_files[idx][14:][:-5].replace("/", ".")
        native.java_test(
            name = test,
            runtime_deps = ["tests"],
            test_class = test,
            testonly = 1,
            visibility = ["//visibility:private"],
            **kwargs
        )
