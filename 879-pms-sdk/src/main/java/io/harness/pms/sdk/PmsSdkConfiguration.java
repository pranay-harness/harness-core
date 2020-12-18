package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.serializer.KryoSerializer;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PmsSdkConfiguration {
  @Builder.Default DeployMode deploymentMode = DeployMode.LOCAL;
  String serviceName;
  MongoConfig mongoConfig;
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig pmsGrpcClientConfig;
  PipelineServiceInfoProvider pipelineServiceInfoProvider;
  FilterCreationResponseMerger filterCreationResponseMerger;
  AsyncWaitEngine asyncWaitEngine;
  KryoSerializer kryoSerializer;
  Map<StepType, Step> engineSteps;

  public enum DeployMode {
    LOCAL,
    REMOTE;
  }
}
