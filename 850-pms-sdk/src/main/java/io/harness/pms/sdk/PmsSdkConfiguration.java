package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;

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
