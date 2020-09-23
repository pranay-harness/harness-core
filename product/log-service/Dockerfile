# Create image for log-service.
#
# Build the log-service image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //...
#
# > docker build -t harness/log-service:valpha-0.1 -f product/log-service/Dockerfile bazel-bin/product/log-service/

FROM alpine:3.12
# Copy go binary
COPY linux_amd64_pure_stripped/log-service /usr/local/bin/log-service
RUN chmod +x /usr/local/bin/log-service
CMD ["/usr/local/bin/log-service", "server"]
