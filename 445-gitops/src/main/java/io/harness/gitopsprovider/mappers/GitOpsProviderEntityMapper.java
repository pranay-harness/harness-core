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

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitOpsProviderEntityMapper {
  GitOpsProviderResponseDTO toGitOpsProvider(@NotNull GitOpsProvider gitopsProvider);
  GitOpsProvider toGitOpsProviderEntity(
      @NotNull GitOpsProviderDTO gitopsProviderDTO, @NotNull String accountIdentifier);
}
