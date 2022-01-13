/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.AccountId;
import io.harness.delegatedetails.DelegateCountRequest;
import io.harness.delegatedetails.DelegateCountResponse;
import io.harness.delegatedetails.DelegateDetailsDescriptor;
import io.harness.delegatedetails.DelegateDetailsDescriptor.Builder;
import io.harness.delegatedetails.DelegateDetailsServiceGrpc.DelegateDetailsServiceBlockingStub;
import io.harness.owner.OrgIdentifier;
import io.harness.owner.ProjectIdentifier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@Singleton
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateDetailsServiceGrpcClient {
  private final DelegateDetailsServiceBlockingStub blockingStub;

  public long getDelegateCount(final String accountId, @Nullable final String orgId, @Nullable final String projectId) {
    final AccountId accountIdentifier = AccountId.newBuilder().setId(accountId).build();
    final Builder descriptorBuilder = DelegateDetailsDescriptor.newBuilder().setAccountId(accountIdentifier);

    if (isNotEmpty(orgId)) {
      descriptorBuilder.setOrgId(OrgIdentifier.newBuilder().setId(orgId).build());
    }
    if (isNotEmpty(projectId)) {
      descriptorBuilder.setProjectId(ProjectIdentifier.newBuilder().setId(projectId).build());
    }
    final DelegateCountResponse response = blockingStub.getDelegateCount(
        DelegateCountRequest.newBuilder().setDelegateDescriptor(descriptorBuilder.build()).build());
    return response.getDelegateCount();
  }
}
