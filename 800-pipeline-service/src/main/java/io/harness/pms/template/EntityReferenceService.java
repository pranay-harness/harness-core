package io.harness.pms.template;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.EntityReferenceRequest;
import io.harness.pms.contracts.service.EntityReferenceResponse;
import io.harness.pms.contracts.service.EntityReferenceServiceGrpc.EntityReferenceServiceImplBase;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
public class EntityReferenceService extends EntityReferenceServiceImplBase {
  @Override
  public void getReferences(EntityReferenceRequest request, StreamObserver<EntityReferenceResponse> responseObserver) {
    EntityReferenceResponse entityReferenceResponse = EntityReferenceResponse.newBuilder().build();

    responseObserver.onNext(entityReferenceResponse);
    responseObserver.onCompleted();
  }
}
