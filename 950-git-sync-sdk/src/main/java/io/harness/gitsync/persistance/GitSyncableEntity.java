package io.harness.gitsync.persistance;

public interface GitSyncableEntity {
  String getBranch();

  void setBranch(String branch);
}
