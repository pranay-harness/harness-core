> test-util.bzl
cat > test-util.bzl <<EOF
DISTRIBUTE_TESTING_WORKER=${DISTRIBUTE_TESTING_WORKER:-0}
DISTRIBUTE_TESTING_WORKERS=${DISTRIBUTE_TESTING_WORKERS:-1}
OPTIMIZED_PACKAGE_TESTS=${OPTIMIZED_PACKAGE_TESTS:-0}
EOF