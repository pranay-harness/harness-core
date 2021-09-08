package io.harness.grpc;

import static com.google.common.base.Strings.emptyToNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegatesetup.DelegateGroupsCountResponse;
import io.harness.delegatesetup.DelegateGroupsRequest;
import io.harness.delegatesetup.DelegateSetupServiceGrpc.DelegateSetupServiceImplBase;
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
public class DelegateSetupServiceGrpcImpl extends DelegateSetupServiceImplBase {
  private final DelegateSetupService delegateSetupService;

  @Override
  public void getDelegateGroupsCount(
      final DelegateGroupsRequest request, final StreamObserver<DelegateGroupsCountResponse> responseObserver) {
    try {
      final String accountId = request.getAccountId().getId();
      final String orgId = emptyToNull(request.getOrgId().getId());
      final String projectId = emptyToNull(request.getProjectId().getId());
      final long delegateGroupCount = delegateSetupService.getDelegateGroupCount(accountId, orgId, projectId);

      responseObserver.onNext(
          DelegateGroupsCountResponse.newBuilder().setDelegateGroupsCount(delegateGroupCount).build());
    } catch (final Exception e) {
      log.error("Unexpected error occurred while getting the number of delegates.", e);
      responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
    }
  }
}
