package io.harness.pms.sdk;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.contracts.plan.PmsServiceGrpc;
import io.harness.pms.contracts.plan.Types;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.execution.listeners.NodeExecutionEventListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventListener;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.execution.SdkOrchestrationEventListener;
import io.harness.queue.QueueListenerController;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkInitHelper {
  public static Map<String, Types> calculateSupportedTypes(PipelineServiceInfoProvider pipelineServiceInfoProvider) {
    List<PartialPlanCreator<?>> planCreators = pipelineServiceInfoProvider.getPlanCreators();
    if (EmptyPredicate.isEmpty(planCreators)) {
      return Collections.emptyMap();
    }

    Map<String, Set<String>> supportedTypes = new HashMap<>();
    for (PartialPlanCreator<?> planCreator : planCreators) {
      Map<String, Set<String>> currTypes = planCreator.getSupportedTypes();
      if (EmptyPredicate.isEmpty(currTypes)) {
        continue;
      }

      currTypes.forEach((k, v) -> {
        if (EmptyPredicate.isEmpty(v)) {
          return;
        }

        if (supportedTypes.containsKey(k)) {
          supportedTypes.get(k).addAll(v);
        } else {
          supportedTypes.put(k, new HashSet<>(v));
        }
      });
    }

    Map<String, Types> finalMap = new HashMap<>();
    supportedTypes.forEach((k, v) -> finalMap.put(k, Types.newBuilder().addAllTypes(v).build()));
    return finalMap;
  }

  public static void initializeSDKInstance(Injector injector, PmsSdkConfiguration pmsSdkConfiguration) {
    initialize(injector, pmsSdkConfiguration);
  }

  private static void initialize(Injector injector, PmsSdkConfiguration config) {
    String serviceName = config.getServiceName();
    log.info("Initializing PMS SDK for service: {}", serviceName);
    if (config.getDeploymentMode().isNonLocal()) {
      ServiceManager serviceManager =
          injector.getInstance(Key.get(ServiceManager.class, Names.named("pmsSDKServiceManager"))).startAsync();
      serviceManager.awaitHealthy();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
      PipelineServiceInfoProvider pipelineServiceInfoProvider = config.getPipelineServiceInfoProviderClass() == null
          ? null
          : injector.getInstance(config.getPipelineServiceInfoProviderClass());
      registerSdk(pipelineServiceInfoProvider, serviceName, injector);
    }
    registerQueueListeners(injector);
  }

  private static void registerQueueListeners(Injector injector) {
    QueueListenerController queueListenerController = injector.getInstance(Key.get(QueueListenerController.class));
    queueListenerController.register(injector.getInstance(NodeExecutionEventListener.class), 1);
    queueListenerController.register(injector.getInstance(SdkOrchestrationEventListener.class), 1);
    queueListenerController.register(injector.getInstance(InterruptEventListener.class), 1);
  }

  private static void registerSdk(
      PipelineServiceInfoProvider pipelineServiceInfoProvider, String serviceName, Injector injector) {
    try {
      StepRegistry stepRegistry = injector.getInstance(StepRegistry.class);
      Map<StepType, Step> registry = stepRegistry.getRegistry();
      List<StepType> stepTypes = registry == null ? Collections.emptyList() : new ArrayList<>(registry.keySet());
      PmsServiceGrpc.PmsServiceBlockingStub pmsClient =
          injector.getInstance(PmsServiceGrpc.PmsServiceBlockingStub.class);
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
