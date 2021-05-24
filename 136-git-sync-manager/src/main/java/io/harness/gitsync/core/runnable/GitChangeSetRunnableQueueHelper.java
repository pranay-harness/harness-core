package io.harness.gitsync.core.runnable;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class GitChangeSetRunnableQueueHelper {
  public YamlChangeSet getQueuedChangeSetForWaitingQueueKey(String accountId, String queueKey,
      int maxRunningChangesetsForAccount, YamlChangeSetService ycsService, List<YamlChangeSetStatus> runningStatusList,
      PersistentLocker persistentLocker) {
    if (accountQuotaMaxedOut(accountId, maxRunningChangesetsForAccount, ycsService, runningStatusList)) {
      log.info("Account quota has been reached. Returning null");
      return null;
    }

    final YamlChangeSet selectedChangeSet =
        selectQueuedChangeSetWithPriority(accountId, queueKey, ycsService, persistentLocker, runningStatusList);

    if (selectedChangeSet == null) {
      log.info("No change set found in queued state");
    }

    return selectedChangeSet;
  }

  private boolean accountQuotaMaxedOut(String accountId, int maxRunningChangesetsForAccount,
      YamlChangeSetService yamlChangeSetService, List<YamlChangeSetStatus> runningStatus) {
    return yamlChangeSetService.countByAccountIdAndStatus(accountId, runningStatus) >= maxRunningChangesetsForAccount;
  }

  private YamlChangeSet selectQueuedChangeSetWithPriority(String accountId, String queueKey,
      YamlChangeSetService ycsService, PersistentLocker persistentLocker, List<YamlChangeSetStatus> runningStatusList) {
    /**
     * Priority of items in queue -
     * 1. BRANCH_CREATE
     * 2. BRANCH_SYNC
     * 3. G2H
     */

    try (AcquiredLock lock = persistentLocker.waitToAcquireLock(
             YamlChangeSet.class, accountId, Duration.ofMinutes(2), Duration.ofSeconds(10))) {
      YamlChangeSet selectedYamlChangeSet = null;
      // Some other instance already picked up and marked status as running skip in that case.
      if (ycsService.changeSetExistsFoQueueKey(accountId, queueKey, runningStatusList)) {
        log.info("Found running changeset for queuekey. Returning null");
        return null;
      }

      final List<YamlChangeSet> changeSets = ycsService.list(queueKey, accountId, YamlChangeSetStatus.QUEUED);

      final List<YamlChangeSet> sortedChangeSets =
          changeSets.stream().sorted(new YamlChangeSetComparator()).collect(Collectors.toList());
      if (isNotEmpty(sortedChangeSets)) {
        selectedYamlChangeSet = sortedChangeSets.get(0);
      }

      if (selectedYamlChangeSet != null) {
        final boolean updateStatus =
            ycsService.updateStatus(accountId, selectedYamlChangeSet.getUuid(), YamlChangeSetStatus.RUNNING);
        if (updateStatus) {
          return ycsService.get(accountId, selectedYamlChangeSet.getUuid()).orElse(null);
        } else {
          log.error("error while updating status of yaml change set Id = [{}]. Skipping selection",
              selectedYamlChangeSet.getUuid());
        }
      }
      return null;
    }
  }

  public class YamlChangeSetComparator implements Comparator<YamlChangeSet> {
    Map<String, Integer> statusOrder = new HashMap<String, Integer>() {
      {
        put(YamlChangeSetEventType.BRANCH_CREATE.name(), 1);
        put(YamlChangeSetEventType.BRANCH_SYNC.name(), 2);
        put(YamlChangeSetEventType.GIT_TO_HARNESS_PUSH.name(), 3);
      }
    };

    @Override
    public int compare(YamlChangeSet o1, YamlChangeSet o2) {
      final String eventType1 = o1.getEventType();
      final String eventType2 = o2.getEventType();

      return statusOrder.get(eventType1) - statusOrder.get(eventType2);
    }
  }
}
