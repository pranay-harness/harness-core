package io.harness.pms.service;

import com.google.inject.Inject;

import io.grpc.stub.StreamObserver;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.beans.entities.PmsSdkInstance;
import io.harness.pms.plan.InitializeSdkRequest;
import io.harness.pms.plan.InitializeSdkResponse;
import io.harness.pms.plan.PmsServiceGrpc.PmsServiceImplBase;
import io.harness.pms.plan.Types;
import io.harness.pms.repository.spring.PmsSdkInstanceRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PmsService extends PmsServiceImplBase {
  private final PmsSdkInstanceRepository pmsSdkInstanceRepository;

  @Inject
  public PmsService(PmsSdkInstanceRepository pmsSdkInstanceRepository) {
    this.pmsSdkInstanceRepository = pmsSdkInstanceRepository;
  }

  public void initializeSdk(InitializeSdkRequest request, StreamObserver<InitializeSdkResponse> responseObserver) {
    saveSdkInstance(request);
    responseObserver.onNext(InitializeSdkResponse.newBuilder().build());
  }

  private void saveSdkInstance(InitializeSdkRequest request) {
    if (EmptyPredicate.isEmpty(request.getName())) {
      throw new InvalidRequestException("Name is empty");
    }

    Map<String, List<String>> supportedTypes = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(request.getSupportedTypesMap())) {
      for (Map.Entry<String, Types> entry : request.getSupportedTypesMap().entrySet()) {
        if (EmptyPredicate.isEmpty(entry.getKey()) || EmptyPredicate.isEmpty(entry.getValue().getTypesList())) {
          continue;
        }
        supportedTypes.put(entry.getKey(),
            entry.getValue().getTypesList().stream().filter(EmptyPredicate::isNotEmpty).collect(Collectors.toList()));
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
}
