package io.harness.ng.core.gitsync;

import io.harness.git.model.ChangeType;
public interface GitSyncManagerInterface {
  // returns YamlChangeSet Id to whose status someone can ping for status
  String processHarnessToGit(ChangeType changeType, String yamlContent, String accountId, String orgId,
      String projectId, String entityName, String entityType, String entityIdentifier);
}
