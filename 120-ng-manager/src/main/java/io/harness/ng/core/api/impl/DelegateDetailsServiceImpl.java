/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.DelegateDetailsServiceGrpcClient;
import io.harness.ng.core.api.DelegateDetailsService;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateDetailsServiceImpl implements DelegateDetailsService {
  private final DelegateDetailsServiceGrpcClient detailsServiceGrpcClient;

  @Override
  public long getDelegateGroupCount(
      final String accountId, @Nullable final String orgId, @Nullable final String projectId) {
    return detailsServiceGrpcClient.getDelegateCount(accountId, orgId, projectId);
  }
}
