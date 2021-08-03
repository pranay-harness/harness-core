package io.harness.pollingframework.server;

import io.harness.grpc.server.GrpcServerConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PollingFrameworkServiceConfiguration {
  GrpcServerConfig grpcServerConfig;
}
