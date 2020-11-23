package io.harness;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.pms.sdk.creator.PlanCreatorProvider;
import io.harness.pms.sdk.creator.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.creator.filters.FilterCreatorProvider;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PmsSdkConfiguration {
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig pmsGrpcClientConfig;
  PlanCreatorProvider planCreatorProvider;
  FilterCreatorProvider filterCreatorProvider;
  FilterCreationResponseMerger filterCreationResponseMerger;
}
