# Create image for ci-lite-engine which will orchestrate steps in a stage
#
# Build the ci-engine docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/engine/...
# > docker build -t harness/ci-lite-engine:<tag> -f product/ci/engine/Dockerfile $(bazel info bazel-bin)/product/ci/engine/

FROM alpine:3.14.2
RUN apk add --update \
    curl>=7.79.0-r0 \
    && rm -rf /var/cache/apk/*
RUN apk add git>=2.30.2
COPY engine_/engine /usr/local/bin/ci-lite-engine
RUN chmod +x /usr/local/bin/ci-lite-engine

RUN adduser -g harness -s /bin/sh -D -u 1000 harness
USER harness:harness

CMD ["/usr/local/bin/ci-lite-engine"]
