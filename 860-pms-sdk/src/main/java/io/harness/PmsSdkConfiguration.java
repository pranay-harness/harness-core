package io.harness;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.pms.sdk.creator.PipelineServiceInfoProvider;
import io.harness.pms.sdk.creator.filters.FilterCreationResponseMerger;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PmsSdkConfiguration {
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig pmsGrpcClientConfig;
  PipelineServiceInfoProvider pipelineServiceInfoProvider;
  FilterCreationResponseMerger filterCreationResponseMerger;
}
