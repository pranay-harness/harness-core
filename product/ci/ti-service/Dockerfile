# Create image for test intelligence service -- Docker hub
#
# Build the test intelligence service image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/ti-service/...
# > cp $(bazel info bazel-bin)/product/ci/ti-service/ti-service_/ti-service  product/ci/ti-service/ti
# > docker build -t harness/ti-service:valpha-0.1 -f product/ci/ti-service/Dockerfile product/ci/

FROM harness/alpine:safe-alpine3.12-sec1338

#copy migrate binary
RUN mkdir /opt/harness
RUN curl -L https://github.com/golang-migrate/migrate/releases/download/v4.14.1/migrate.linux-amd64.tar.gz | tar xvz && cp migrate.linux-amd64 /opt/harness/migrate
RUN chmod +x /opt/harness/migrate

# Copy go binary
COPY ti-service/ti /opt/harness/
COPY ti-service/migrations/ /opt/harness/migrations/
COPY ti-service/scripts/ /opt/harness/

RUN chmod +x /opt/harness/*.sh

RUN addgroup -S 65534 && adduser -S 65534 -G 65534
RUN chown -R 65534:65534 /opt/harness/
USER 65534

WORKDIR /opt/harness

CMD ["./ti", "server"]
