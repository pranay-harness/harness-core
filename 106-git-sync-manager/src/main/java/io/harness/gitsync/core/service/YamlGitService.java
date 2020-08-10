package io.harness.gitsync.core.service;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;

import java.util.List;

public interface YamlGitService {
  YamlGitConfigDTO weNeedToPushChanges(
      String accountId, String orgIdentifier, String projectIdentifier, String rootFolder);

  YamlGitConfigDTO getYamlGitConfigForHarnessToGitChangeSet(
      GitFileChange harnessToGitFileChange, YamlChangeSet harnessToGitChangeSet);

  void handleHarnessChangeSet(YamlChangeSet yamlChangeSet, String accountId);

  void removeGitSyncErrors(
      String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList, boolean gitToHarness);
}
