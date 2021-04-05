package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;

@OwnedBy(DX)
public interface GitSyncableEntity extends NGAccess {
  String getObjectId();
}
