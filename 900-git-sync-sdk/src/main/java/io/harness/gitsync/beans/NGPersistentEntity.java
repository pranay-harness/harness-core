package io.harness.gitsync.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(DX)
public interface NGPersistentEntity {
  String getLastCommitId();
}
