package io.harness;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.pms.plan.creator.filters.FilterCreationResponseMerger;
import io.harness.pms.plan.creator.plan.PipelineServiceInfoProvider;

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
