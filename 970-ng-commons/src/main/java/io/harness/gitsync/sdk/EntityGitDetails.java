package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(DX)
public class EntityGitDetails {
  String objectId;
  String branch;
  String repoIdentifier;
  String rootFolder;
  String filePath;

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
