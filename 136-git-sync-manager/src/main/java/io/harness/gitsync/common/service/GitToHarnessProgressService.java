package io.harness.gitsync.common.service;

import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.GitToHarnessProgress;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import java.util.List;
import org.springframework.data.mongodb.core.query.Update;

public interface GitToHarnessProgressService {
  GitToHarnessProgress save(GitToHarnessProgress gitToHarnessProgress);

  GitToHarnessProgress update(String uuid, Update update);

  void updateFilesInProgressRecord(String uuid, List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess);

  GitToHarnessProgress updateStatus(String uuid, GitToHarnessProcessingStepStatus stepStatus);

  GitToHarnessProgress save(YamlChangeSetDTO yamlChangeSetDTO, YamlChangeSetEventType eventType,
      GitToHarnessProcessingStepType stepType, GitToHarnessProcessingStepStatus stepStatus);
}
