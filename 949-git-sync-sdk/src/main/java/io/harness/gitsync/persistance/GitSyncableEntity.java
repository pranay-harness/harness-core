package io.harness.gitsync.persistance;

import io.harness.ng.core.NGAccess;

public interface GitSyncableEntity extends NGAccess {
  String getObjectId();
}
