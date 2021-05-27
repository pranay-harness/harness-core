package io.harness.pms.sdk;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.SdkDeployMode;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.redis.RedisConfig;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class PmsSdkConfiguration {
  @Builder.Default SdkDeployMode deploymentMode = SdkDeployMode.LOCAL;
  String serviceName;
  MongoConfig mongoConfig;
  GrpcServerConfig grpcServerConfig;
  GrpcClientConfig pmsGrpcClientConfig;
  Class<? extends PipelineServiceInfoProvider> pipelineServiceInfoProviderClass;
  FilterCreationResponseMerger filterCreationResponseMerger;
  Map<StepType, Class<? extends Step>> engineSteps;
  Map<AdviserType, Class<? extends Adviser>> engineAdvisers;
  Map<FacilitatorType, Class<? extends Facilitator>> engineFacilitators;
  Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap;
  Class<? extends ExecutionSummaryModuleInfoProvider> executionSummaryModuleInfoProviderClass;
  @Default
  EventsFrameworkConfiguration eventsFrameworkConfiguration =
      EventsFrameworkConfiguration.builder()
          .redisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
          .build();
}
