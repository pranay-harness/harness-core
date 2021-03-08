package io.harness.gitsync.server;

import io.harness.grpc.server.GrpcServerConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncServiceConfiguration {
  GrpcServerConfig grpcServerConfig;
}
