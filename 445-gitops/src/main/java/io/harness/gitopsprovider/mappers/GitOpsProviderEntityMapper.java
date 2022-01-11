/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
  GitOpsProviderResponseDTO toGitOpsProviderDTO(@NotNull GitOpsProvider gitopsProvider);
  GitOpsProvider toGitOpsProviderEntity(
      @NotNull GitOpsProviderDTO gitopsProviderDTO, @NotNull String accountIdentifier);
}
