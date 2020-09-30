# Create image for ci-addon which will be run as an entrypoint in customer
# container
#
# Build the ci-addon docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/addon/...
# > docker build -t harness/ci-addon:<tag> -f product/ci/addon/Dockerfile bazel-bin/product/ci/addon/


# First stage
FROM alpine:3.12
# Copy ci-addon binary
COPY linux_amd64_pure_stripped/ci-addon /usr/local/bin/ci-addon-linux-amd64
RUN chmod +x /usr/local/bin/ci-addon-linux-amd64
CMD ["/usr/local/bin/ci-addon-linux-amd64"]
