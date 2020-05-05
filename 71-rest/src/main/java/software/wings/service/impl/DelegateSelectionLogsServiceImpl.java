package software.wings.service.impl;

import static software.wings.beans.FeatureName.DELEGATE_SELECTION_LOG;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateSelectionLog;
import software.wings.beans.DelegateSelectionLog.DelegateSelectionLogBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.FeatureFlagService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Singleton
@Slf4j
public class DelegateSelectionLogsServiceImpl implements DelegateSelectionLogsService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;

  private static final String WAITING_FOR_APPROVAL = "Waiting for Approval";
  private static final String DISCONNECTED = "Disconnected";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";

  @Override
  public void save(BatchDelegateSelectionLog batch) {
    if (batch == null || batch.getDelegateSelectionLogs().isEmpty()) {
      return;
    }
    try {
      if (featureFlagService.isEnabled(
              DELEGATE_SELECTION_LOG, batch.getDelegateSelectionLogs().iterator().next().getAccountId())) {
        wingsPersistence.save(batch.getDelegateSelectionLogs());
      }
      wingsPersistence.save(batch.getDelegateSelectionLogs());
      logger.info("Batch saved successfully");
    } catch (Exception exception) {
      logger.error("Error while saving into Database ", exception);
    }
  }

  @Override
  public BatchDelegateSelectionLog createBatch(String taskId) {
    if (taskId == null) {
      return null;
    }
    return BatchDelegateSelectionLog.builder().taskId(taskId).build();
  }

  private DelegateSelectionLogBuilder retrieveDelegateSelectionLogBuilder(
      String accountId, String taskId, Set<String> delegateIds) {
    return DelegateSelectionLog.builder().accountId(accountId).taskId(taskId).delegateIds(delegateIds);
  }

  @Override
  public void logCanAssign(BatchDelegateSelectionLog batch, String accountId, String delegateId) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(SELECTED)
                     .message("Successfully matched scopes and selectors at " + LocalDateTime.now())
                     .build());
  }

  @Override
  public void logNoIncludeScopeMatched(BatchDelegateSelectionLog batch, String accountId, String delegateId) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED).message("No matching include scope").build());
  }

  @Override
  public void logExcludeScopeMatched(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, DelegateScope scope) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(
        delegateSelectionLogBuilder.conclusion(REJECTED).message("Matched exclude scope " + scope.getName()).build());
  }

  @Override
  public void logMissingSelector(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, String selector) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED).message("Missing selector " + selector).build());
  }

  @Override
  public void logMissingAllSelectors(BatchDelegateSelectionLog batch, String accountId, String delegateId) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED).message("Missing all selectors").build());
  }

  @Override
  public void logDisconnectedDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds) {
    if (batch == null) {
      return;
    }

    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(DISCONNECTED)
                     .message("Delegate was disconnected at " + LocalDateTime.now())
                     .build());
  }

  @Override
  public void logWaitingForApprovalDelegate(
      BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds) {
    if (batch == null) {
      return;
    }

    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(WAITING_FOR_APPROVAL)
                     .message("Delegate was waiting for approval at " + LocalDateTime.now())
                     .build());
  }
}