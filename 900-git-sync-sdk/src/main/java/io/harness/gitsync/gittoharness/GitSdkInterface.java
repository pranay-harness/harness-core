package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.EntityInfo;

@OwnedBy(DX)
public interface GitSdkInterface {
  /**
   * Throws exception in case it is unable to process a changeset
   */
  void process(ChangeSet changeSet);

  boolean markEntityInvalid(String accountId, EntityInfo entityInfo);
}
