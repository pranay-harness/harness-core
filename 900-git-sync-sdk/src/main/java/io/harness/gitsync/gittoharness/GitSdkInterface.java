package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;

@OwnedBy(DX)
public interface GitSdkInterface {
  /**
   * Throws exception in case it is unable to process a changeset
   */
  void process(ChangeSet changeSet);
}
