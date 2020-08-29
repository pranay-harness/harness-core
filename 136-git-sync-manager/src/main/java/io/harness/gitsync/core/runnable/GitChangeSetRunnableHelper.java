package io.harness.gitsync.core.runnable;

import static io.harness.gitsync.common.beans.YamlChangeSet.MAX_QUEUE_DURATION_EXCEEDED_CODE;

import com.google.inject.Singleton;

import com.mongodb.client.result.UpdateResult;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.core.service.YamlChangeSetService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Singleton
@Slf4j
public class GitChangeSetRunnableHelper {
  private static final long TIMEOUT_FOR_RUNNING_CHANGESET = 90;
  private static final long TIMEOUT_FOR_MARKING_SKIPPED = 3 /*days*/;

  public List<YamlChangeSet> getStuckYamlChangeSets(
      YamlChangeSetService yamlChangeSetService, List<String> runningAccountIdList) {
    return yamlChangeSetService.findByAccountIdsStatusLastUpdatedAtLessThan(
        runningAccountIdList, TIMEOUT_FOR_RUNNING_CHANGESET);
  }

  public List<String> getRunningAccountIdList(YamlChangeSetService yamlChangeSetService) {
    return yamlChangeSetService.findDistinctAccountIdsByStatus(Status.RUNNING);
  }

  public void handleOldQueuedChangeSets(YamlChangeSetService yamlChangeSetService) {
    logger.info("Marking obsolete queued changesets as skipped");
    try {
      final UpdateResult update =
          yamlChangeSetService.updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(Status.QUEUED,
              Status.SKIPPED, System.currentTimeMillis() - Duration.ofDays(TIMEOUT_FOR_MARKING_SKIPPED).toMillis(),
              MAX_QUEUE_DURATION_EXCEEDED_CODE);
      logger.info("Successfully marked obsolete queued changesets with update results = [{}]", update);
    } catch (Exception e) {
      logger.error("Error while marking obsolete queued changesets as skipped", e);
    }
  }
}
