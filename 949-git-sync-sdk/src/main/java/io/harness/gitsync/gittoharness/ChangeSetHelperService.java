package io.harness.gitsync.gittoharness;

import io.harness.gitsync.ChangeSet;

public interface ChangeSetHelperService {
  /**
   * Throws exception in case it is unable to process a changeset
   */
  void process(ChangeSet changeSet);
}
