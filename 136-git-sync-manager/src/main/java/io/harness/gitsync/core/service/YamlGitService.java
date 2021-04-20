package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;

import java.util.List;

@OwnedBy(DX)
public interface YamlGitService {
  YamlGitConfigDTO weNeedToPushChanges(
      String accountId, String orgIdentifier, String projectIdentifier, String rootFolder);

  YamlGitConfigDTO getYamlGitConfigForHarnessToGitChangeSet(
      GitFileChange harnessToGitFileChange, YamlChangeSet harnessToGitChangeSet);

  void handleGitChangeSet(YamlChangeSet yamlChangeSets, String accountId);

  void removeGitSyncErrors(String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList);

  List<YamlGitConfigDTO> getYamlGitConfigsForGitToHarnessChangeSet(YamlChangeSet gitToHarnessChangeSet);

  void processFailedChanges(String accountId, List<ChangeWithErrorMsg> failedChangesWithErrorMsg,
      YamlGitConfigDTO yamlGitConfig, boolean fullSync);
}
