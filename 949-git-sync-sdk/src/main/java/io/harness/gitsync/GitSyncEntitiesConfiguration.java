package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.persistance.GitSyncableEntity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncEntitiesConfiguration {
  Class<? extends GitSyncableEntity> entityClass;
  Class<? extends NGDTO> yamlClass;
  Class<? extends EntityGitPersistenceHelperService<? extends GitSyncableEntity, ? extends NGDTO>> entityHelperClass;
}
