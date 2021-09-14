/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gitopsprovider.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.ConnectedArgoGitOpsInfoDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.ConnectedArgoProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.GITOPS)
public class ConnectedGitOpsProviderEntityMapper implements GitOpsProviderEntityMapper {
  @Override
  public GitOpsProviderResponseDTO toGitOpsProvider(@NotNull GitOpsProvider gitopsProvider) {
    return GitOpsProviderResponseDTO.builder()
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder()
                     .adapterUrl(((ConnectedArgoProvider) gitopsProvider).getAdapterUrl())
                     .build())
        .identifier(gitopsProvider.getIdentifier())
        .orgIdentifier(gitopsProvider.getOrgIdentifier())
        .projectIdentifier(gitopsProvider.getProjectIdentifier())
        .build();
  }

  @Override
  public GitOpsProvider toGitOpsProviderEntity(
      @NotNull GitOpsProviderDTO gitopsProviderDTO, @NotNull String accountIdentifier) {
    final ConnectedArgoProvider entity =
        ConnectedArgoProvider.builder()
            .adapterUrl(((ConnectedArgoGitOpsInfoDTO) gitopsProviderDTO.getInfoDTO()).getAdapterUrl())
            .build();
    entity.setIdentifier(gitopsProviderDTO.getIdentifier());
    entity.setAccountIdentifier(accountIdentifier);
    entity.setProjectIdentifier(gitopsProviderDTO.getProjectIdentifier());
    entity.setOrgIdentifier(gitopsProviderDTO.getOrgIdentifier());
    return entity;
  }
}
