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
import io.harness.delegate.beans.connector.gitops.ManagedArgoGitOpsInfoDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.entity.ManagedGitOpsProvider;

@OwnedBy(HarnessTeam.GITOPS)
public class ManagedGitOpsProviderEntityMapper extends AbstractGitOpsProviderEntityMapper {
  @Override
  public GitOpsProviderResponseDTO toGitOpsProviderDTO(GitOpsProvider gitopsProvider) {
    ManagedGitOpsProvider m = (ManagedGitOpsProvider) gitopsProvider;
    ManagedArgoGitOpsInfoDTO infoDTO = ManagedArgoGitOpsInfoDTO.builder().namespace(m.getNamespace()).build();
    GitOpsProviderResponseDTO responseDTO = new GitOpsProviderResponseDTO();
    responseDTO.setInfoDTO(infoDTO);
    setDtoFields(gitopsProvider, responseDTO);
    return responseDTO;
  }

  @Override
  public GitOpsProvider toGitOpsProviderEntity(GitOpsProviderDTO gitopsProviderDTO, String accountIdentifier) {
    ManagedArgoGitOpsInfoDTO m = (ManagedArgoGitOpsInfoDTO) gitopsProviderDTO.getInfoDTO();
    final ManagedGitOpsProvider entity = ManagedGitOpsProvider.builder().namespace(m.getNamespace()).build();
    setEntityFields(gitopsProviderDTO, entity, accountIdentifier);
    return entity;
  }
}
