package io.harness.pms.sdk;

import io.harness.PmsSdkCoreModule;
import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.core.execution.NodeExecutionEventListener;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.execution.SdkOrchestrationEventListener;
import io.harness.pms.sdk.registries.PmsSdkRegistryModule;
import io.harness.queue.QueueListenerController;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
    String serviceName = config.getServiceName();
    Injector injector = Guice.createInjector(getModules(serviceName));
    if (config.getDeploymentMode().equals(DeployMode.REMOTE)) {
      ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
      serviceManager.awaitHealthy();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
      registerSdk(pipelineServiceInfoProvider, serviceName, injector);
      registerEventListeners(injector);
    }
  }

  @NotNull
  private List<Module> getModules(String serviceName) {
    List<Module> modules = new ArrayList<>();
    modules.add(PmsSdkCoreModule.getInstance());
    if (config.getDeploymentMode().equals(DeployMode.REMOTE)) {
      modules.add(PmsSdkRegistryModule.getInstance(config));
      modules.add(PmsSdkPersistenceModule.getInstance());
      modules.add(PmsSdkProviderModule.getInstance(config));
      modules.add(PmsSdkGrpcModule.getInstance(config));
      modules.add(PmsSdkQueueModule.getInstance(config));
    }
    return modules;
  }

  private void registerEventListeners(Injector injector) {
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(NodeExecutionEventListener.class), 1);
    queueListenerController.register(injector.getInstance(SdkOrchestrationEventListener.class), 1);
  }

  private void registerSdk(
      PipelineServiceInfoProvider pipelineServiceInfoProvider, String serviceName, Injector injector) {
    try {
      StepRegistry stepRegistry = injector.getInstance(StepRegistry.class);
      Map<StepType, Step> registry = stepRegistry.getRegistry();
      List<StepType> stepTypes = registry == null ? Collections.emptyList() : new ArrayList<>(registry.keySet());
      PmsServiceBlockingStub pmsClient = injector.getInstance(PmsServiceBlockingStub.class);
      pmsClient.initializeSdk(
          InitializeSdkRequest.newBuilder()
              .setName(serviceName)
              .putAllSupportedTypes(PmsSdkInitHelper.calculateSupportedTypes(pipelineServiceInfoProvider))
              .addAllSupportedSteps(pipelineServiceInfoProvider.getStepInfo())
              .addAllSupportedStepTypes(stepTypes)
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
