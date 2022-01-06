/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitOpsProviderCustomRepository {
  GitOpsProvider get(
      String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier);
  boolean delete(String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier);
  Page<GitOpsProvider> findAll(Pageable pageable, String projectIdentifier, String orgIdentifier,
      String accountIdentifier, String searchTerm, GitOpsProviderType type);
  GitOpsProvider save(GitOpsProvider gitopsProvider);
  GitOpsProvider update(String accountIdentifier, GitOpsProvider gitopsProvider);
}
