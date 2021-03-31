package io.harness.gitsync;

import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.gitsync.persistance.GitSyncableEntity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncEntitiesConfiguration {
  Class<? extends GitSyncableEntity> entityClass;
  Class<? extends NGDTO> yamlClass;
  Class<? extends EntityGitPersistenceHelperService<? extends GitSyncableEntity, ? extends NGDTO>> entityHelperClass;
}
