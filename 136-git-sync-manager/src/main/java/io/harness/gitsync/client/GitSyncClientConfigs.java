package io.harness.gitsync.client;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.client.GrpcClientConfig;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncClientConfigs {
  Map<ModuleType, GrpcClientConfig> gitSyncGrpcClients;
}
