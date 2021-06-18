package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@OwnedBy(DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitToHarnessProgressHelper {
  private GitToHarnessProgressService gitToHarnessProgressService;

  public YamlChangeSetStatus getQueueStatusIfEventInProgressOrAlreadyProcessed(YamlChangeSetDTO yamlChangeSetDTO) {
    GitToHarnessProgressDTO gitToHarnessProgressDTO =
        gitToHarnessProgressService.getByRepoUrlAndCommitIdAndEventType(yamlChangeSetDTO.getRepoUrl(),
            yamlChangeSetDTO.getGitWebhookRequestAttributes().getHeadCommitId(), yamlChangeSetDTO.getEventType());

    if (gitToHarnessProgressDTO != null) {
      if (gitToHarnessProgressDTO.getGitToHarnessProgressStatus().isInProcess()) {
        return YamlChangeSetStatus.RUNNING;
      }
      if (gitToHarnessProgressDTO.getGitToHarnessProgressStatus().isSuccessStatus()) {
        return YamlChangeSetStatus.COMPLETED;
      }
    }
    return null;
  }
}
