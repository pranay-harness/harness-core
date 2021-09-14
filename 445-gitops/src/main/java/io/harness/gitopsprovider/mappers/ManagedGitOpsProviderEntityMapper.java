/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gitopsprovider.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;

@OwnedBy(HarnessTeam.GITOPS)
public class ManagedGitOpsProviderEntityMapper implements GitOpsProviderEntityMapper {
  @Override
  public GitOpsProviderResponseDTO toGitOpsProvider(GitOpsProvider gitopsProvider) {
    return null;
  }

  @Override
  public GitOpsProvider toGitOpsProviderEntity(GitOpsProviderDTO gitopsProviderDTO, String accountIdentifier) {
    return null;
  }
}
