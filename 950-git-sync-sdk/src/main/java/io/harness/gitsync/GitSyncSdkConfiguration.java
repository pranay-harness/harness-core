package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.grpc.client.GrpcClientConfig;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncSdkConfiguration {
  GrpcClientConfig grpcClientConfig;
  Supplier<List<EntityType>> gitSyncSortOrder;
}
