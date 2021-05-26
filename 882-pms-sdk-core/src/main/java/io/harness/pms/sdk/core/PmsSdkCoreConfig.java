package io.harness.pms.sdk.core;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PmsSdkCoreConfig {
  SdkDeployMode sdkDeployMode;
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig grpcClientConfig;
}
