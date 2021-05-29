package io.harness.pms.sdk.core;

import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionServiceImpl;
import io.harness.pms.sdk.core.interrupt.PMSInterruptService;
import io.harness.pms.sdk.core.interrupt.PMSInterruptServiceGrpcImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeGrpcServiceImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingGrpcOutputService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.response.publishers.MongoSdkResponseEventPublisher;
import io.harness.pms.sdk.response.publishers.RedisSdkResponseEventPublisher;
import io.harness.pms.sdk.response.publishers.SdkResponseEventPublisher;
import io.harness.pms.utils.PmsManagedService;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Set;

public class PmsSdkCoreModule extends AbstractModule {
  private static PmsSdkCoreModule instance;
  private final PmsSdkCoreConfig config;

  public static PmsSdkCoreModule getInstance(PmsSdkCoreConfig config) {
    if (instance == null) {
      instance = new PmsSdkCoreModule(config);
    }
    return instance;
  }

  private PmsSdkCoreModule(PmsSdkCoreConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    if (config.getSdkDeployMode().isNonLocal()) {
      install(PmsSdkGrpcModule.getInstance(config));
    } else {
      install(PmsSdkDummyGrpcModule.getInstance());
    }

    install(PmsSdkQueueModule.getInstance(config));
    bind(PMSInterruptService.class).to(PMSInterruptServiceGrpcImpl.class).in(Singleton.class);
    bind(OutcomeService.class).to(OutcomeGrpcServiceImpl.class).in(Singleton.class);
    bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingGrpcOutputService.class).in(Singleton.class);
    bind(SdkNodeExecutionService.class).to(SdkNodeExecutionServiceImpl.class).in(Singleton.class);
    install(
        PmsSdkCoreEventsFrameworkModule.getInstance(config.getEventsFrameworkConfiguration(), config.getServiceName()));
    if (config.isUseRedisForSdkResponseEvents()) {
      bind(SdkResponseEventPublisher.class).to(RedisSdkResponseEventPublisher.class);
    } else {
      bind(SdkResponseEventPublisher.class).to(MongoSdkResponseEventPublisher.class);
    }
  }

  @Provides
  @Singleton
  @Named("pmsManagedServiceManager")
  public ServiceManager serviceManager(@Named("pmsManagedServices") Set<PmsManagedService> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME)
  public String serviceName() {
    return config.getServiceName();
  }
}
