package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "EntityGitDetails", description = "This contains Git Details of the Entity")
@OwnedBy(DX)
public class EntityGitDetails {
  @Schema(description = "Object Id of the Entity") String objectId;
  @Schema(description = "Branch Name") String branch;
  @Schema(description = "Git Sync Config Id") String repoIdentifier;
  @Schema(description = "Root Folder Path of the Entity") String rootFolder;
  @Schema(description = "File Path of the Entity") String filePath;
  @Schema(description = "Name of the repo") String repoName;

  public GitSyncBranchContext toGitSyncBranchContext() {
    GitEntityInfo gitEntityInfo = GitEntityInfo.builder()
                                      .branch(branch)
                                      .yamlGitConfigId(repoIdentifier)
                                      .lastObjectId(objectId)
                                      .folderPath(rootFolder)
                                      .filePath(filePath)
                                      .build();
    return GitSyncBranchContext.builder().gitBranchInfo(gitEntityInfo).build();
  }
}
