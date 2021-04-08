package io.harness.pms.sdk;

import static io.harness.pms.sdk.PmsSdkConfiguration.DeployMode.REMOTE;
import static io.harness.pms.sdk.PmsSdkConfiguration.DeployMode.REMOTE_IN_PROCESS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.MongoConfig;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.interrupt.PMSInterruptService;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.resolver.expressions.EngineGrpcExpressionService;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeGrpcServiceImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingGrpcOutputService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.execution.PmsNodeExecutionServiceGrpcImpl;
import io.harness.pms.sdk.interrupt.PMSInterruptServiceGrpcImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
class PmsSdkProviderModule extends AbstractModule {
  private final PmsSdkConfiguration config;

  private static PmsSdkProviderModule instance;

  public static PmsSdkProviderModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkProviderModule(config);
    }
    return instance;
  }

  private PmsSdkProviderModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(PMSInterruptService.class).to(PMSInterruptServiceGrpcImpl.class).in(Singleton.class);
    bind(PmsNodeExecutionService.class).to(PmsNodeExecutionServiceGrpcImpl.class).in(Singleton.class);
    if (config.getDeploymentMode() == REMOTE) {
      bind(EngineExpressionService.class).to(EngineGrpcExpressionService.class).in(Singleton.class);
    }

    if (config.getDeploymentMode() == REMOTE || config.getDeploymentMode() == REMOTE_IN_PROCESS) {
      bind(OutcomeService.class).to(OutcomeGrpcServiceImpl.class).in(Singleton.class);
      bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingGrpcOutputService.class).in(Singleton.class);
    }

    if (config.getExecutionSummaryModuleInfoProviderClass() != null) {
      bind(ExecutionSummaryModuleInfoProvider.class)
          .to(config.getExecutionSummaryModuleInfoProviderClass())
          .in(Singleton.class);
    }
    if (config.getPipelineServiceInfoProviderClass() != null) {
      bind(PipelineServiceInfoProvider.class).to(config.getPipelineServiceInfoProviderClass()).in(Singleton.class);
    }
  }

  @Provides
  @Singleton
  public FilterCreationResponseMerger filterCreationResponseMerger() {
    return config.getFilterCreationResponseMerger();
  }
  //
  //  @Provides
  //  @Singleton
  //  public ServiceManager serviceManager(Set<Service> services) {
  //    return new ServiceManager(services);
  //  }

  @Provides
  @Singleton
  @Named("pmsSdkMongoConfig")
  public MongoConfig mongoConfig() {
    return config.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME)
  public String serviceName() {
    return config.getServiceName();
  }
}
