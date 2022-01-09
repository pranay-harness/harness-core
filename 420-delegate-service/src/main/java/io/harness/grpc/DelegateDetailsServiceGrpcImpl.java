/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import static com.google.common.base.Strings.emptyToNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegatedetails.DelegateCountRequest;
import io.harness.delegatedetails.DelegateCountResponse;
import io.harness.delegatedetails.DelegateDetailsServiceGrpc.DelegateDetailsServiceImplBase;
import io.harness.service.intfc.DelegateSetupService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateDetailsServiceGrpcImpl extends DelegateDetailsServiceImplBase {
  private final DelegateSetupService delegateSetupService;

  @Override
  public void getDelegateCount(
      final DelegateCountRequest request, final StreamObserver<DelegateCountResponse> responseObserver) {
    try {
      final String accountId = request.getDelegateDescriptor().getAccountId().getId();
      final String orgId = emptyToNull(request.getDelegateDescriptor().getOrgId().getId());
      final String projectId = emptyToNull(request.getDelegateDescriptor().getProjectId().getId());
      final long delegateGroupCount = delegateSetupService.getDelegateGroupCount(accountId, orgId, projectId);

      responseObserver.onNext(DelegateCountResponse.newBuilder().setDelegateCount(delegateGroupCount).build());
      responseObserver.onCompleted();
    } catch (final Exception e) {
      log.error("Unexpected error occurred while getting the number of delegates.", e);
      responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
    }
  }
}
