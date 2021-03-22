package io.harness.gitsync.server;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.server.GrpcServerConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncServiceConfiguration {
  GrpcServerConfig grpcServerConfig;
}
