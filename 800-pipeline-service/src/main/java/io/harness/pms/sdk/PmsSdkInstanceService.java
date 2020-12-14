package io.harness.pms.sdk;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.contracts.plan.InitializeSdkResponse;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceImplBase;
import io.harness.pms.contracts.plan.Types;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.repositories.sdk.PmsSdkInstanceRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class PmsSdkInstanceService extends PmsServiceImplBase {
  private final PmsSdkInstanceRepository pmsSdkInstanceRepository;

  @Inject
  public PmsSdkInstanceService(PmsSdkInstanceRepository pmsSdkInstanceRepository) {
    this.pmsSdkInstanceRepository = pmsSdkInstanceRepository;
  }

  @Override
  public void initializeSdk(InitializeSdkRequest request, StreamObserver<InitializeSdkResponse> responseObserver) {
    saveSdkInstance(request);
    responseObserver.onNext(InitializeSdkResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  private void saveSdkInstance(InitializeSdkRequest request) {
    if (EmptyPredicate.isEmpty(request.getName())) {
      throw new InvalidRequestException("Name is empty");
    }

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
      pmsSdkInstanceRepository.updatePmsSdkInstance(request.getName(), supportedTypes, request.getSupportedStepsList());
    } else {
      pmsSdkInstanceRepository.save(PmsSdkInstance.builder()
                                        .name(request.getName())
                                        .supportedTypes(supportedTypes)
                                        .supportedSteps(request.getSupportedStepsList())
                                        .build());
    }
  }

  public Map<String, Map<String, Set<String>>> getInstanceNameToSupportedTypes() {
    Map<String, Map<String, Set<String>>> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(
        instance -> instances.put(instance.getName(), instance.getSupportedTypes()));
    return instances;
  }

  public Map<String, List<StepInfo>> getInstanceNameToSupportedSteps() {
    Map<String, List<StepInfo>> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(
        instance -> instances.put(instance.getName(), instance.getSupportedSteps()));
    return instances;
  }
}
