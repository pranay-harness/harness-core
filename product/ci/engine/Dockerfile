# Create image for ci-lite-engine which will be run as an entrypoint in customer
# container
#
# Build the ci-engine docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //...
# > docker build -t harness/ci-lite-engine:<tag> -f product/ci/engine/Dockerfile bazel-bin/product/ci/engine/


# First stage
FROM alpine:latest
# Copy ci-lite-engine binary
COPY linux_amd64_pure_stripped/engine /usr/local/bin/ci-lite-engine-linux-amd64
RUN chmod +x /usr/local/bin/ci-lite-engine-linux-amd64
CMD ["/usr/local/bin/ci-lite-engine-linux-amd64"]