# Create image for ci-addon which will be run as sidecar in buildfarm
# Build the ci-addon binary as: go build GOOS=linux GOARCH=amd64 -o ci-addon
# The above build step is temporary until its updated in Bazel.
FROM alpine:latest
COPY ci-addon /tmp/ci-addon
RUN chmod +x /tmp/ci-addon
CMD ["/tmp/ci-addon"]
