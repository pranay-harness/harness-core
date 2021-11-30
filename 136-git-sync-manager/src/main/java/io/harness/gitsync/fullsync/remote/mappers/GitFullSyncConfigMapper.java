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
        .message(gitFullSyncConfig.getMessage())
        .createPullRequest(gitFullSyncConfig.isCreatePullRequest())
        .repoIdentifier(gitFullSyncConfig.getYamlGitConfigIdentifier())
        .accountIdentifier(gitFullSyncConfig.getAccountIdentifier())
        .orgIdentifier(gitFullSyncConfig.getOrgIdentifier())
        .projectIdentifier(gitFullSyncConfig.getProjectIdentifier())
        .build();
  }

  public static GitFullSyncConfig fromDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull GitFullSyncConfigRequestDTO gitFullSyncConfigRequestDTO) {
    return GitFullSyncConfig.builder()
        .baseBranch(gitFullSyncConfigRequestDTO.getBaseBranch())
        .message(gitFullSyncConfigRequestDTO.getMessage())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .branch(gitFullSyncConfigRequestDTO.getBranch())
        .yamlGitConfigIdentifier(gitFullSyncConfigRequestDTO.getRepoIdentifier())
        .createPullRequest(gitFullSyncConfigRequestDTO.isCreatePullRequest())
        .build();
  }
}
