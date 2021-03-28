package io.harness.gitsync.persistance;

import io.harness.EntityType;
import io.harness.ng.core.EntityDetail;

public interface GitSyncableEntity {
  String getBranch();

  void setBranch(String branch);

  EntityType getEntityType();

  EntityDetail getEntityDetail();
}
