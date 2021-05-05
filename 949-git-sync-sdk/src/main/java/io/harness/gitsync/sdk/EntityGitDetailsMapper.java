package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncableEntity;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class EntityGitDetailsMapper {
  public EntityGitDetails mapEntityGitDetails(GitSyncableEntity gitSyncableEntity) {
    return EntityGitDetails.builder()
        .branch(gitSyncableEntity.getBranch())
        .objectId(gitSyncableEntity.getObjectIdOfYaml())
        .repoIdentifier(gitSyncableEntity.getYamlGitConfigRef())
        .rootFolder(gitSyncableEntity.getRootFolder())
        .filePath(gitSyncableEntity.getFilePath())
        .build();
  }
}
