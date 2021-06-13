package io.harness.pms.sdk;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.contracts.plan.InitializeSdkResponse;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceImplBase;
import io.harness.pms.contracts.plan.Types;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.exception.InitializeSdkException;
import io.harness.pms.pipeline.StepPalleteInfo;
import io.harness.repositories.sdk.PmsSdkInstanceRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PmsSdkInstanceService extends PmsServiceImplBase {
  private static final String LOCK_NAME_PREFIX = "PmsSdkInstanceService-";

  private final PmsSdkInstanceRepository pmsSdkInstanceRepository;
  private final PersistentLocker persistentLocker;

  @Inject
  public PmsSdkInstanceService(PmsSdkInstanceRepository pmsSdkInstanceRepository, PersistentLocker persistentLocker) {
    this.pmsSdkInstanceRepository = pmsSdkInstanceRepository;
    this.persistentLocker = persistentLocker;
  }

  @Override
  public void initializeSdk(InitializeSdkRequest request, StreamObserver<InitializeSdkResponse> responseObserver) {
    if (EmptyPredicate.isEmpty(request.getName())) {
      throw new InvalidRequestException("Name is empty");
    }

    try (AcquiredLock<?> lock =
             persistentLocker.tryToAcquireLock(LOCK_NAME_PREFIX + request.getName(), Duration.ofMinutes(2))) {
      if (lock == null) {
        throw new InitializeSdkException("Could not acquire lock");
      }
      saveSdkInstance(request);
    }
    responseObserver.onNext(InitializeSdkResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  private void saveSdkInstance(InitializeSdkRequest request) {
    Map<String, Set<String>> supportedTypes = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(request.getSupportedTypesMap())) {
      for (Map.Entry<String, Types> entry : request.getSupportedTypesMap().entrySet()) {
        if (EmptyPredicate.isEmpty(entry.getKey()) || EmptyPredicate.isEmpty(entry.getValue().getTypesList())) {
          continue;
        }
        supportedTypes.put(entry.getKey(),
            entry.getValue().getTypesList().stream().filter(EmptyPredicate::isNotEmpty).collect(Collectors.toSet()));
      }
    }

    Optional<PmsSdkInstance> instanceOptional = pmsSdkInstanceRepository.findByName(request.getName());
    if (instanceOptional.isPresent()) {
      pmsSdkInstanceRepository.updatePmsSdkInstance(request, supportedTypes);
    } else {
      pmsSdkInstanceRepository.save(PmsSdkInstance.builder()
                                        .name(request.getName())
                                        .supportedTypes(supportedTypes)
                                        .supportedSteps(request.getSupportedStepsList())
                                        .supportedStepTypes(request.getSupportedStepTypesList())
                                        .sdkModuleInfo(request.getSdkModuleInfo())
                                        .interruptConsumerConfig(request.getInterruptConsumerConfig())
                                        .orchestrationEventConsumerConfig(request.getOrchestrationEventConsumerConfig())
                                        .facilitatorEventConsumerConfig(request.getFacilitatorEventConsumerConfig())
                                        .nodeStartEventConsumerConfig(request.getNodeStartEventConsumerConfig())
                                        .build());
    }
  }

  public Map<String, Map<String, Set<String>>> getInstanceNameToSupportedTypes() {
    Map<String, Map<String, Set<String>>> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(
        instance -> instances.put(instance.getName(), instance.getSupportedTypes()));
    return instances;
  }

  public Map<String, StepPalleteInfo> getModuleNameToStepPalleteInfo() {
    Map<String, StepPalleteInfo> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(instance
        -> instances.put(instance.getName(),
            StepPalleteInfo.builder()
                .moduleName(instance.getSdkModuleInfo().getDisplayName())
                .stepTypes(instance.getSupportedSteps())
                .build()));
    return instances;
  }

  public Map<String, List<StepType>> getInstanceNameToSupportedStepTypes() {
    Map<String, List<StepType>> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(
        instance -> instances.put(instance.getName(), instance.getSupportedStepTypes()));
    return instances;
  }
  public Set<String> getInstanceNames() {
    Set<String> instanceNames = new HashSet<>();
    pmsSdkInstanceRepository.findAll().forEach(instance -> instanceNames.add(instance.getName()));
    return instanceNames;
  }
}
