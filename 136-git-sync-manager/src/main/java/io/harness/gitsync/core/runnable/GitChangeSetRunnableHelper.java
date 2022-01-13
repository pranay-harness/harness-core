/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.runnable;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.beans.YamlChangeSet.MAX_QUEUE_DURATION_EXCEEDED_CODE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetService;

import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitChangeSetRunnableHelper {
  private static final long TIMEOUT_FOR_MARKING_SKIPPED = 3 /*days*/;

  public List<YamlChangeSetDTO> getStuckYamlChangeSets(YamlChangeSetService yamlChangeSetService,
      List<String> runningAccountIdList, List<YamlChangeSetStatus> runningStatusList) {
    return yamlChangeSetService.findByAccountIdsStatusCutoffLessThan(
        runningAccountIdList, runningStatusList, System.currentTimeMillis());
  }

  public List<String> getRunningAccountIdList(
      YamlChangeSetService yamlChangeSetService, List<YamlChangeSetStatus> yamlChangeSetStatuses) {
    return yamlChangeSetService.findDistinctAccountIdsByStatus(yamlChangeSetStatuses);
  }

  public void handleOldQueuedChangeSets(YamlChangeSetService yamlChangeSetService) {
    log.info("Marking obsolete queued changesets as skipped");
    try {
      final UpdateResult update =
          yamlChangeSetService.updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
              YamlChangeSetStatus.QUEUED, YamlChangeSetStatus.SKIPPED,
              System.currentTimeMillis() - Duration.ofDays(TIMEOUT_FOR_MARKING_SKIPPED).toMillis(),
              MAX_QUEUE_DURATION_EXCEEDED_CODE);
      log.info("Successfully marked obsolete queued changesets with update results = [{}]", update);
    } catch (Exception e) {
      log.error("Error while marking obsolete queued changesets as skipped", e);
    }
  }
}
