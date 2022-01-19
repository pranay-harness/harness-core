/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.remote.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncConfig;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigRequestDTO;

import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class GitFullSyncConfigMapper {
  public static GitFullSyncConfigDTO toDTO(@NotNull GitFullSyncConfig gitFullSyncConfig) {
    return GitFullSyncConfigDTO.builder()
        .branch(gitFullSyncConfig.getBranch())
        .baseBranch(gitFullSyncConfig.getBaseBranch())
        .prTitle(gitFullSyncConfig.getPrTitle())
        .createPullRequest(gitFullSyncConfig.isCreatePullRequest())
        .repoIdentifier(gitFullSyncConfig.getYamlGitConfigIdentifier())
        .accountIdentifier(gitFullSyncConfig.getAccountIdentifier())
        .orgIdentifier(gitFullSyncConfig.getOrgIdentifier())
        .projectIdentifier(gitFullSyncConfig.getProjectIdentifier())
        .targetBranch(gitFullSyncConfig.getTargetBranch())
        .isNewBranch(gitFullSyncConfig.isNewBranch())
        .rootFolder(gitFullSyncConfig.getRootFolder())
        .build();
  }

  public static GitFullSyncConfig fromDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull GitFullSyncConfigRequestDTO gitFullSyncConfigRequestDTO) {
    return GitFullSyncConfig.builder()
        .baseBranch(gitFullSyncConfigRequestDTO.getBaseBranch())
        .prTitle(gitFullSyncConfigRequestDTO.getPrTitle())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .branch(gitFullSyncConfigRequestDTO.getBranch())
        .yamlGitConfigIdentifier(gitFullSyncConfigRequestDTO.getRepoIdentifier())
        .createPullRequest(gitFullSyncConfigRequestDTO.isCreatePullRequest())
        .targetBranch(gitFullSyncConfigRequestDTO.getTargetBranch())
        .isNewBranch(gitFullSyncConfigRequestDTO.isNewBranch())
        .rootFolder(gitFullSyncConfigRequestDTO.getRootFolder())
        .build();
  }
}
