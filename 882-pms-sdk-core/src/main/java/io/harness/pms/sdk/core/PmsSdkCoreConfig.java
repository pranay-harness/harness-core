/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core;

import io.harness.AuthorizationServiceHeader;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.redis.RedisConfig;
import io.harness.threading.ThreadPoolConfig;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkCoreConfig {
  String serviceName;
  SdkDeployMode sdkDeployMode;
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig grpcClientConfig;
  ThreadPoolConfig executionPoolConfig;
  ThreadPoolConfig orchestrationEventPoolConfig;

  @Default
  EventsFrameworkConfiguration eventsFrameworkConfiguration =
      EventsFrameworkConfiguration.builder()
          .redisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
          .build();
  AuthorizationServiceHeader serviceHeader;
}
