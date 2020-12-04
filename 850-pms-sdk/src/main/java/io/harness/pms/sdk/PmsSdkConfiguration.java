package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.registries.state.StepRegistry;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PmsSdkConfiguration {
  MongoConfig mongoConfig;
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig pmsGrpcClientConfig;
  PipelineServiceInfoProvider pipelineServiceInfoProvider;
  FilterCreationResponseMerger filterCreationResponseMerger;
  StepRegistry stepRegistry;
}
