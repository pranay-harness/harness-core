# Create image for ci-addon which will be run as sidecar in buildfarm
#
# Build the ci-addon docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //...
# > docker build -t harness/ci-addon:valpha-0.1 -f product/ci/addon/Dockerfile bazel-bin/product/ci/addon/


# First stage
FROM alpine:latest
# Install jfrog cli with version 1.35.5
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*
RUN curl -fL https://getcli.jfrog.io | sh /dev/stdin 1.35.5
RUN mv ./jfrog /usr/local/bin/
# Copy ci-addon binary
COPY linux_amd64_pure_stripped/ci-addon /usr/local/bin/ci-addon

# Second stage
FROM gcr.io/kaniko-project/executor:debug
COPY --from=0 /usr/local/bin/jfrog /usr/local/bin/jfrog
COPY --from=0 /usr/local/bin/ci-addon /usr/local/bin/ci-addon
RUN ["chmod", "+x", "/usr/local/bin/ci-addon"]
ENTRYPOINT ["/busybox/sh", "-c"]
CMD ["/usr/local/bin/ci-addon"]