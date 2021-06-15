package io.harness.gitsync.common.service;

import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import java.util.List;
import org.springframework.data.mongodb.core.query.Update;

public interface GitToHarnessProgressService {
  GitToHarnessProgressDTO save(GitToHarnessProgressDTO gitToHarnessProgress);

  GitToHarnessProgressDTO update(String uuid, Update update);

  GitToHarnessProgressDTO updateFilesInProgressRecord(
      String uuid, List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess);

  GitToHarnessProgressDTO updateStatus(String uuid, GitToHarnessProcessingStepStatus stepStatus);

  GitToHarnessProgressDTO save(YamlChangeSetDTO yamlChangeSetDTO, YamlChangeSetEventType eventType,
      GitToHarnessProcessingStepType stepType, GitToHarnessProcessingStepStatus stepStatus);

  GitToHarnessProgressDTO startNewStep(String gitToHarnessProgressRecordId,
      GitToHarnessProcessingStepType processFilesInMsvs, GitToHarnessProcessingStepStatus status);

  GitToHarnessProgressDTO updateProgressWithProcessingResponse(
      String gitToHarnessProgressRecordId, GitToHarnessProcessingResponse gitToHarnessResponse);
}
