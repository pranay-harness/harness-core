package io.harness.gitsync.entityInfo;

import io.harness.EntityType;
import io.harness.gitsync.beans.NGDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.EntityDetail;

import java.util.function.Supplier;

public interface EntityGitPersistenceHelperService<B extends GitSyncableEntity, Y extends NGDTO> {
  //   Supplier<Y>  getYamlFromEntity(B entity);

  EntityType getEntityType();

  Supplier<B> getEntityFromYaml(Y yaml);

  EntityDetail getEntityDetail(B entity);
}
