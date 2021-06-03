package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.GitToHarnessProgress;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import com.google.inject.Inject;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitToHarnessStatusRecordHelper {
  @Inject private GitToHarnessProgressService gitToHarnessProgressService;

  public GitToHarnessProgress saveGitToHarnessStatusRecord(YamlChangeSetDTO yamlChangeSetDTO,
      YamlChangeSetEventType eventType, GitToHarnessProcessingStepType stepType,
      GitToHarnessProcessingStepStatus stepStatus) {
    GitToHarnessProgress gitToHarnessProgress = GitToHarnessProgress.builder()
                                                    .accountIdentifier(yamlChangeSetDTO.getAccountId())
                                                    .yamlChangeSetId(yamlChangeSetDTO.getChangesetId())
                                                    .repoUrl(yamlChangeSetDTO.getRepoUrl())
                                                    .branch(yamlChangeSetDTO.getBranch())
                                                    .eventType(eventType)
                                                    .stepType(stepType)
                                                    .stepStatus(stepStatus)
                                                    .stepStartingTime(System.currentTimeMillis())
                                                    .build();
    return gitToHarnessProgressService.save(gitToHarnessProgress);
  }

  public GitToHarnessProgress saveGitToHarnessStatusRecord(GitToHarnessProgress existingRecord,
      GitToHarnessProcessingStepType stepType, GitToHarnessProcessingStepStatus stepStatus) {
    GitToHarnessProgress gitToHarnessProgress = GitToHarnessProgress.builder()
                                                    .accountIdentifier(existingRecord.getAccountIdentifier())
                                                    .yamlChangeSetId(existingRecord.getYamlChangeSetId())
                                                    .repoUrl(existingRecord.getRepoUrl())
                                                    .branch(existingRecord.getBranch())
                                                    .eventType(existingRecord.getEventType())
                                                    .stepType(stepType)
                                                    .stepStatus(stepStatus)
                                                    .stepStartingTime(System.currentTimeMillis())
                                                    .build();
    return gitToHarnessProgressService.save(gitToHarnessProgress);
  }
}
