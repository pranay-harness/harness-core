package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.persistance.GitSyncableEntity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncEntitiesConfiguration {
  Class<? extends GitSyncableEntity> entityClass;
  Class<? extends YamlDTO> yamlClass;
  Class<? extends EntityGitPersistenceHelperService<? extends GitSyncableEntity, ? extends YamlDTO>> entityHelperClass;
}
