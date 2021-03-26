package io.harness.gitsync;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.redis.RedisConfig;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncSdkConfiguration {
  /**
   * Grpc server config which sdk will start.
   */
  GrpcServerConfig grpcServerConfig;
  /**
   * client to communicate to gms server.
   */
  Microservice microservice;
  GrpcClientConfig grpcClientConfig;
  Supplier<List<EntityType>> gitSyncSortOrder;
  RedisConfig eventsRedisConfig;
  DeployMode deployMode;

  public enum DeployMode { REMOTE, IN_PROCESS }
}
