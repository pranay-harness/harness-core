/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitToHarnessProgressConstants;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitToHarnessProgressHelper {
  private GitToHarnessProgressService gitToHarnessProgressService;

  public YamlChangeSetStatus getQueueStatusIfEventInProgressOrAlreadyProcessed(YamlChangeSetDTO yamlChangeSetDTO) {
    // TODO
    //  currently processing on basis of yamlChangeSetId, relying that queue will make sure that
    //  no 2 events of same repo-commitid-eventType are processed together
    //  Should handle it properly later and remove this dependency
    GitToHarnessProgressDTO gitToHarnessProgressDTO =
        gitToHarnessProgressService.getByYamlChangeSetId(yamlChangeSetDTO.getChangesetId());
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

  public void doPreRunChecks(YamlChangeSetDTO yamlChangeSetDTO) {
    GitToHarnessProgressDTO gitToHarnessProgressDTO =
        gitToHarnessProgressService.getByYamlChangeSetId(yamlChangeSetDTO.getChangesetId());
    if (gitToHarnessProgressDTO != null && !gitToHarnessProgressDTO.getGitToHarnessProgressStatus().isSuccessStatus()) {
      // Check if the event has been long running over than threshold duration without reaching completion
      // Mark it as failed if thats true, so that it can be performed again
      if (System.currentTimeMillis() - gitToHarnessProgressDTO.getLastUpdatedAt()
          >= GitToHarnessProgressConstants.longRunningEventResetDurationInMs) {
        gitToHarnessProgressService.updateProgressStatus(
            gitToHarnessProgressDTO.getUuid(), GitToHarnessProgressStatus.ERROR);
        log.info("Updating the status to {} as the event has been processing for more than threshold duration",
            GitToHarnessProgressStatus.ERROR);
      }
    }
  }
}
