package io.harness.gitsync.gitsyncerror.service;

import java.util.List;

public interface GitSyncErrorService {
  void deleteByAccountIdOrgIdProjectIdAndFilePath(
      String accountId, String orgId, String projectId, List<String> yamlFilePath);
}
