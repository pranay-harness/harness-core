package io.harness.gitsync.core.service;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;

public interface YamlGitService {
  YamlGitConfigDTO weNeedToPushChanges(
      String accountId, String orgIdentifier, String projectIdentifier, String rootFolder);

  YamlGitConfigDTO getYamlGitConfigForHarnessToGitChangeSet(
      GitFileChange harnessToGitFileChange, YamlChangeSet harnessToGitChangeSet);

  void handleHarnessChangeSet(YamlChangeSet yamlChangeSet, String accountId);

  void removeGitSyncErrors(
      String accountId, String orgId, String projectId, List<GitFileChange> gitFileChangeList, boolean gitToHarness);

  void handleGitChangeSet(YamlChangeSet yamlChangeSets, String accountId);

  String validateAndQueueWebhookRequest(
      String accountId, String webhookToken, String yamlWebHookPayload, HttpHeaders headers);

  void processFailedChanges(String accountId, List<ChangeWithErrorMsg> failedChangesWithErrorMsg,
      YamlGitConfigDTO yamlGitConfig, boolean gitToHarness, boolean fullSync);

  List<YamlGitConfigDTO> getYamlGitConfigsForGitToHarnessChangeSet(YamlChangeSet gitToHarnessChangeSet);
}
