/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
