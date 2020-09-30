# Create image for ci-lite-engine which will orchestrate steps in a stage
#
# Build the ci-engine docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/engine/...
# > docker build -t harness/ci-lite-engine:<tag> -f product/ci/engine/Dockerfile bazel-bin/product/ci/engine/

# First stage
FROM alpine:3.12
# Install jfrog cli with version 1.35.5
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*
RUN curl -fL https://getcli.jfrog.io | sh /dev/stdin 1.35.5
RUN mv ./jfrog /usr/local/bin/
# Copy ci-lite-engine binary
COPY linux_amd64_pure_stripped/engine /usr/local/bin/ci-lite-engine

# Second stage
FROM gcr.io/kaniko-project/executor:debug-v0.16.0
COPY --from=0 /usr/local/bin/jfrog /usr/local/bin/jfrog
COPY --from=0 /usr/local/bin/ci-lite-engine /usr/local/bin/ci-lite-engine
RUN ["chmod", "+x", "/usr/local/bin/ci-lite-engine"]
ENTRYPOINT ["/busybox/sh", "-c"]
CMD ["/usr/local/bin/ci-lite-engine"]