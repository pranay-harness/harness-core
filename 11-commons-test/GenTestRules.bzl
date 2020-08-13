def run_tests(deps,test_files,srcs,**kwargs):
    for idx in range(len(test_files)):
        temp = test_files[idx].split('/')[-1][:-5]
        native.java_test(name=temp,srcs = srcs,deps = deps,**kwargs)