package io.harness.pms.sdk;

import io.harness.mongo.MongoConfig;
import io.harness.pms.sdk.core.execution.ExecutionSummaryModuleInfoProvider;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeGrpcServiceImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingGrpcOutputService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.pms.sdk.execution.PmsNodeExecutionServiceGrpcImpl;
import io.harness.serializer.KryoSerializer;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Set;

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
    bind(PmsNodeExecutionService.class).to(PmsNodeExecutionServiceGrpcImpl.class).in(Singleton.class);
    bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingGrpcOutputService.class).in(Singleton.class);
    bind(OutcomeService.class).to(OutcomeGrpcServiceImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  public PipelineServiceInfoProvider pipelineServiceInfoProvider() {
    return config.getPipelineServiceInfoProvider();
  }

  @Provides
  @Singleton
  public ExecutionSummaryModuleInfoProvider moduleInfoProvider() {
    return config.getExecutionSummaryModuleInfoProvider();
  }

  @Provides
  @Singleton
  public FilterCreationResponseMerger filterCreationResponseMerger() {
    return config.getFilterCreationResponseMerger();
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("pmsSdkMongoConfig")
  public MongoConfig mongoConfig() {
    return config.getMongoConfig();
  }

  @Provides
  @Singleton
  public AsyncWaitEngine asyncWaitEngine() {
    return config.getAsyncWaitEngine();
  }

  @Provides
  @Singleton
  public KryoSerializer kryoSerializer() {
    return config.getKryoSerializer();
  }
}
