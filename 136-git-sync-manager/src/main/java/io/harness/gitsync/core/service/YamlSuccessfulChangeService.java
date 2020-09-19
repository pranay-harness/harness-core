package io.harness.gitsync.core.service;

import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;

import java.util.Optional;

public interface YamlSuccessfulChangeService {
  String upsert(YamlSuccessfulChange yamlSuccessfulChange);

  void updateOnHarnessChangeSet(YamlChangeSet savedYamlChangeset);

  void updateOnSuccessfulGitChangeProcessing(
      GitFileChange gitFileChange, String accountId, String orgId, String projectId);

  Optional<YamlSuccessfulChange> get(String accountId, String orgId, String projectId, String filePath);
}
