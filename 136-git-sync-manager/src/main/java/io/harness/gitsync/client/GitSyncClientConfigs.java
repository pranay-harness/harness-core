package io.harness.gitsync.client;

import io.harness.ModuleType;
import io.harness.grpc.client.GrpcClientConfig;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncClientConfigs {
  Map<ModuleType, GrpcClientConfig> gitSyncGrpcClients;
}
