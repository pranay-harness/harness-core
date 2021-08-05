package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.DelegateSetupServiceGrpcClient;
import io.harness.ng.core.api.DelegateDetailsService;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateDetailsServiceImpl implements DelegateDetailsService {
  private final DelegateSetupServiceGrpcClient setupServiceGrpcClient;

  @Override
  public long getDelegateGroupCount(final String accountId, final String orgId, final String projectId) {
    return setupServiceGrpcClient.getDelegateGroupsCount(accountId, orgId, projectId);
  }
}
