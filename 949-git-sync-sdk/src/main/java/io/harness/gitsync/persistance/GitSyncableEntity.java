package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;
import io.harness.persistence.PersistentEntity;

@OwnedBy(DX)
public interface GitSyncableEntity extends NGAccess, PersistentEntity {
  /**
   * Add object Id to the entity class and lombok annotation should automatically generate getter.
   * Also it is important that the field name is <b>objectId</b>.
   */
  String getObjectIdOfYaml();

  void setObjectIdOfYaml(String objectId);

  void setIsFromDefaultBranch(Boolean isDefault);

  Boolean getIsFromDefaultBranch();

  String getYamlGitConfigId();

  void setYamlGitConfigId(String objectId);
}
