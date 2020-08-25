# Create image for ci-scm service
#
# Build the ci-scm docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/scm/...
# > docker build -t harness/ci-scm:<tag> -f product/ci/scm/Dockerfile bazel-bin/product/ci/scm/linux_amd64_pure_stripped


# First stage
FROM alpine:3.12
# Copy ci-scm binary
COPY scm /usr/local/bin/scm
RUN chmod +x /usr/local/bin/scm
CMD ["/usr/local/bin/scm"]
