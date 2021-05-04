package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitEntityUtils {
  public String getFilePathInGit(GitSyncEntityDTO gitSyncEntityDTO) {
    return gitSyncEntityDTO.getFolderPath() + gitSyncEntityDTO.getEntityGitPath();
  }
}
