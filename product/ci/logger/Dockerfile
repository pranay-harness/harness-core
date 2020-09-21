# Create image for logger which will wrap around any command to be executed on the container and write logs to log service.

# Build the ci-logger docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/...
# > docker build -t harness/ci-logger:<tag> -f product/ci/logger/Dockerfile bazel-bin/product/ci/logger/

FROM alpine/git
# Copy logger binary
COPY linux_amd64_pure_stripped/logger /usr/local/bin/ci-logger
RUN chmod +x /usr/local/bin/ci-logger
ENTRYPOINT ["/bin/sh", "-c"]

