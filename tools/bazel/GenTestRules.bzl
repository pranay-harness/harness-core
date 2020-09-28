def run_tests(deps, test_files, **kwargs):
    for idx in range(len(test_files)):
        temp = test_files[idx].split("/")[-1][:-5]
        temp1 = [test_files[idx]]
        native.java_test(name = temp, srcs = temp1, deps = deps, resources = temp1, **kwargs)

def run_harness_tests(test_files, deps = None, **kwargs):
    if deps == None:
        deps = []

    deps.append("//990-commons-test:module")
    deps.append("//:lombok")
    deps.append("@maven//:com_google_code_findbugs_annotations")
    deps.append("@maven//:com_google_guava_guava")
    deps.append("@maven//:org_assertj_assertj_core")
    deps.append("module")
    deps.append("tests")

    run_tests(deps, test_files, **kwargs)
