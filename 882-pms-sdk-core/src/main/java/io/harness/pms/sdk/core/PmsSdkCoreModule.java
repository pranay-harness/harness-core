package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionServiceImpl;
import io.harness.pms.sdk.core.interrupt.publisher.RedisSdkInterruptEventNotifyPublisher;
import io.harness.pms.sdk.core.interrupt.publisher.SdkInterruptEventNotifyPublisher;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeGrpcServiceImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingGrpcOutputService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.response.publishers.RedisSdkResponseEventPublisher;
import io.harness.pms.sdk.core.response.publishers.SdkResponseEventPublisher;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PIPELINE)
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

    bind(OutcomeService.class).to(OutcomeGrpcServiceImpl.class).in(Singleton.class);
    bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingGrpcOutputService.class).in(Singleton.class);
    bind(SdkNodeExecutionService.class).to(SdkNodeExecutionServiceImpl.class).in(Singleton.class);
    bind(SdkResponseEventPublisher.class).to(RedisSdkResponseEventPublisher.class);
    bind(SdkInterruptEventNotifyPublisher.class).to(RedisSdkInterruptEventNotifyPublisher.class);
  }

  @Provides
  @Singleton
  @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME)
  public String serviceName() {
    return config.getServiceName();
  }

  @Provides
  @Singleton
  @Named(PmsSdkModuleUtils.SDK_EXECUTOR_NAME)
  public ExecutorService sdkExecutorService() {
    return ThreadPool.create(5, 20, 30L, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("PmsSdkOrchestrationEventListener-%d").build());
  }

  @Provides
  @Singleton
  public PmsSdkCoreConfig pmsSdkCoreConfig() {
    return config;
  }
}
