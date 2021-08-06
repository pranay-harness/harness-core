package io.harness.grpc;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.AccountId;
import io.harness.delegatesetup.DelegateGroupsCountResponse;
import io.harness.delegatesetup.DelegateGroupsRequest;
import io.harness.delegatesetup.DelegateSetupServiceGrpc.DelegateSetupServiceBlockingStub;
import io.harness.owner.OrgIdentifier;
import io.harness.owner.ProjectIdentifier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@Singleton
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateSetupServiceGrpcClient {
  private final DelegateSetupServiceBlockingStub blockingStub;

  public long getDelegateGroupsCount(
      final String accountId, @Nullable final String orgId, @Nullable final String projectId) {
    final AccountId accountIdentifier = AccountId.newBuilder().setId(accountId).build();
    final OrgIdentifier orgIdentifier = OrgIdentifier.newBuilder().setId(orgId).build();
    final ProjectIdentifier projectIdentifier = ProjectIdentifier.newBuilder().setId(projectId).build();

    final DelegateGroupsRequest request = DelegateGroupsRequest.newBuilder()
                                              .setAccountId(accountIdentifier)
                                              .setOrgId(orgIdentifier)
                                              .setProjectId(projectIdentifier)
                                              .build();
    final DelegateGroupsCountResponse response = blockingStub.getDelegateGroupsCount(request);

    return response.getDelegateGroupsCount();
  }
}
