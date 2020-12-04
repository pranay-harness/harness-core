package io.harness.pms.sdk;

import io.harness.pms.plan.InitializeSdkRequest;
import io.harness.pms.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("ALL")
public class PmsSdkModule {
  private static PmsSdkModule defaultInstance;

  public static PmsSdkModule getDefaultInstance() {
    return defaultInstance;
  }

  public static void initializeDefaultInstance(PmsSdkConfiguration config) throws Exception {
    if (defaultInstance == null) {
      defaultInstance = new PmsSdkModule(config);
      defaultInstance.initialize();
    }
  }

  private final PmsSdkConfiguration config;

  private PmsSdkModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  private void initialize() throws StatusRuntimeException, Exception {
    PipelineServiceInfoProvider pipelineServiceInfoProvider = config.getPipelineServiceInfoProvider();
    String serviceName = pipelineServiceInfoProvider.getServiceName();

    Injector injector = Guice.createInjector(PmsSdkGrpcModule.getInstance(), PmsSdkPersistenceModule.getInstance(),
        PmsSdkProviderModule.getInstance(config, serviceName), PmsSdkQueueModule.getInstance());

    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
    registerSdk(pipelineServiceInfoProvider, serviceName, injector);
  }

  private void registerSdk(
      PipelineServiceInfoProvider pipelineServiceInfoProvider, String serviceName, Injector injector) {
    try {
      PmsServiceBlockingStub pmsClient = injector.getInstance(PmsServiceBlockingStub.class);
      pmsClient.initializeSdk(
          InitializeSdkRequest.newBuilder()
              .setName(serviceName)
              .putAllSupportedTypes(PmsSdkInitHelper.calculateSupportedTypes(pipelineServiceInfoProvider))
              .addAllSupportedSteps(pipelineServiceInfoProvider.getStepInfo())
              .build());
    } catch (StatusRuntimeException ex) {
      log.error("Sdk Initialization failed with StatusRuntimeException Status: {}", ex.getStatus());
      throw ex;
    } catch (Exception ex) {
      log.error("Sdk Initialization failed with Status: {}", ex.getMessage());
      throw ex;
    }
  }
}
