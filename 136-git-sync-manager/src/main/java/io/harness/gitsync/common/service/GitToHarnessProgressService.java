package io.harness.gitsync.common.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import java.util.List;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.DX)
public interface GitToHarnessProgressService {
  GitToHarnessProgressDTO save(GitToHarnessProgressDTO gitToHarnessProgress);

  GitToHarnessProgressDTO update(String uuid, Update update);

  GitToHarnessProgressDTO updateFilesInProgressRecord(
      String uuid, List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess);

  GitToHarnessProgressDTO updateStepStatus(String uuid, GitToHarnessProcessingStepStatus stepStatus);

  GitToHarnessProgressDTO save(YamlChangeSetDTO yamlChangeSetDTO, YamlChangeSetEventType eventType,
      GitToHarnessProcessingStepType stepType, GitToHarnessProcessingStepStatus stepStatus);

  GitToHarnessProgressDTO startNewStep(String gitToHarnessProgressRecordId,
      GitToHarnessProcessingStepType processFilesInMsvs, GitToHarnessProcessingStepStatus status);

  GitToHarnessProgressDTO updateProgressWithProcessingResponse(
      String gitToHarnessProgressRecordId, GitToHarnessProcessingResponse gitToHarnessResponse);

  boolean isProgressEventAlreadyProcessedOrInProcess(String repoURL, String commitId, YamlChangeSetEventType eventType);

  GitToHarnessProgressDTO initProgress(YamlChangeSetDTO yamlChangeSetDTO, YamlChangeSetEventType eventType,
      GitToHarnessProcessingStepType stepType, String commitId);

  GitToHarnessProgressDTO updateProgressStatus(
      String gitToHarnessProgressRecordId, GitToHarnessProgressStatus gitToHarnessProgressStatus);

  GitToHarnessProgressDTO getBranchSyncStatus(String repoURL, String branch);

  GitToHarnessProgressDTO getByRepoUrlAndCommitIdAndEventType(
      String repoURL, String commitId, YamlChangeSetEventType eventType);

  GitToHarnessProgressDTO getByYamlChangeSetId(String yamlChangeSetId);

  GitToHarnessProgressDTO updateProcessingCommitId(String uuid, String processingCommitId);
}
