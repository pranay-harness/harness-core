def run_tests(deps, test_files, **kwargs):
    for idx in range(len(test_files)):
        temp = test_files[idx].split("/")[-1][:-5]
        temp1 = [test_files[idx]]
        native.java_test(name = temp, srcs = temp1, deps = deps, resources = temp1, **kwargs)
