package io.harness.gitsync.entityInfo;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.ng.core.EntityDetail;

import java.util.List;
import java.util.function.Supplier;

@OwnedBy(DX)
public interface GitSdkEntityHandlerInterface<B extends GitSyncableEntity, Y extends YamlDTO> {
  Supplier<Y> getYamlFromEntity(B entity);

  EntityType getEntityType();

  Supplier<B> getEntityFromYaml(Y yaml, String accountIdentifier);

  EntityDetail getEntityDetail(B entity);

  Y save(String accountIdentifier, String yaml);

  Y update(String accountIdentifier, String yaml, ChangeType changeType);

  boolean delete(EntityReference entityReference);

  String getObjectIdOfYamlKey();

  String getIsFromDefaultBranchKey();

  String getYamlGitConfigRefKey();

  String getUuidKey();

  String getBranchKey();

  Y upsert(String accountIdentifier, String yaml);

  Y fullSyncEntity(FullSyncChangeSet fullSyncChangeSet);

  List<FileChange> listAllEntities(ScopeDetails scopeDetails);
}
