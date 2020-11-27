package io.harness.pms.sdk;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.plan.InitializeSdkRequest;
import io.harness.pms.plan.InitializeSdkResponse;
import io.harness.pms.plan.PmsServiceGrpc.PmsServiceImplBase;
import io.harness.pms.plan.Types;
import io.harness.repositories.sdk.PmsSdkInstanceRepository;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
      pmsSdkInstanceRepository.updateSupportedTypes(request.getName(), supportedTypes);
    } else {
      pmsSdkInstanceRepository.save(
          PmsSdkInstance.builder().name(request.getName()).supportedTypes(supportedTypes).build());
    }
  }

  public Map<String, Map<String, Set<String>>> getSdkInstancesMap() {
    Map<String, Map<String, Set<String>>> instances = new HashMap<>();
    pmsSdkInstanceRepository.findAll().forEach(
        instance -> instances.put(instance.getName(), instance.getSupportedTypes()));
    return instances;
  }
}
