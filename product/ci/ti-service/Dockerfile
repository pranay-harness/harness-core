# Create image for test intelligence service.
#
# Build the test intelligence service image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/...
#
# > docker build -t harness/ti-service:valpha-0.1 -f product/ci/ti-service/Dockerfile bazel-bin/product/ci/ti-service/

FROM alpine:3.12
# Copy go binary
COPY linux_amd64_pure_stripped/ti-service /usr/local/bin/ti-service
RUN addgroup -S 65534 && adduser -S 65534 -G 65534
RUN chown -R 65534:65534 /usr/local/bin
USER 65534
CMD ["/usr/local/bin/ti-service", "server"]
