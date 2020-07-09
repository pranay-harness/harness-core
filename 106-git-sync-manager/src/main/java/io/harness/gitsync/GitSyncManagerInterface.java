package io.harness.gitsync;

import io.harness.gitsync.common.beans.Change.ChangeType;

public interface GitSyncManagerInterface {
  boolean processHarnessToGit(ChangeType changeType, String yamlContent, String yamlPath, String accountId,
      String orgId, String projectId, String entityId);
}
