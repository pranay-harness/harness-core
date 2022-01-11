/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.gitopsprovider.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitopsProviderCrudService {
  Optional<GitOpsProviderResponseDTO> get(@NotNull String accountIdentifier, String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String connectorIdentifier);

  Page<GitOpsProviderResponseDTO> list(Pageable pageable, @NotNull String accountIdentifier, String orgIdentifier,
      @NotNull String projectIdentifier, String searchTerm, GitOpsProviderType type);

  GitOpsProviderResponseDTO create(@NotNull GitOpsProviderDTO connector, @NotNull String accountIdentifier);

  GitOpsProviderResponseDTO update(@NotNull GitOpsProviderDTO connector, @NotNull String accountIdentifier);

  boolean delete(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String connectorIdentifier);
}
