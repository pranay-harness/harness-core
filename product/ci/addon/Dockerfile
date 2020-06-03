# Create image for ci-addon which will be run as sidecar in buildfarm
# Build the ci-addon binary as: go build GOOS=linux GOARCH=amd64 -o ci-addon
# The above build step is temporary until its updated in Bazel.

# First stage
FROM alpine:latest
# Install jfrog cli with version 1.35.5
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*
RUN curl -fL https://getcli.jfrog.io | sh /dev/stdin 1.35.5
RUN mv ./jfrog /usr/local/bin/
# Copy ci-addon binary
COPY ci-addon /tmp/ci-addon

# Second stage
FROM gcr.io/kaniko-project/executor:debug
COPY --from=0 /usr/local/bin/jfrog /usr/local/bin/jfrog
COPY --from=0 /tmp/ci-addon /tmp/ci-addon
RUN ["chmod", "+x", "/tmp/ci-addon"]
ENTRYPOINT ["/busybox/sh", "-c"]
CMD ["/tmp/ci-addon"]