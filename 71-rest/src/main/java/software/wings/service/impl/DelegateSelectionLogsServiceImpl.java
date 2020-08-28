package software.wings.service.impl;

import static software.wings.beans.FeatureName.DISABLE_DELEGATE_SELECTION_LOG;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogBuilder;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogKeys;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class DelegateSelectionLogsServiceImpl implements DelegateSelectionLogsService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private DelegateService delegateService;

  private static final String WAITING_FOR_APPROVAL = "Waiting for Approval";
  private static final String DISCONNECTED = "Disconnected";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";
  private static final String ACCEPTED = "Accepted";

  private static final String CAN_ASSIGN_GROUP_ID = "CAN_ASSIGN_GROUP_ID";
  private static final String NO_INCLUDE_SCOPE_MATCHED_GROUP_ID = "NO_INCLUDE_SCOPE_MATCHED_GROUP_ID";
  private static final String EXCLUDE_SCOPE_MATCHED_GROUP_ID = "EXCLUDE_SCOPE_MATCHED_GROUP_ID";
  private static final String MISSING_SELECTOR_GROUP_ID = "MISSING_SELECTOR_GROUP_ID";
  private static final String MISSING_ALL_SELECTORS_GROUP_ID = "MISSING_ALL_SELECTORS_GROUP_ID";
  private static final String DISCONNECTED_GROUP_ID = "DISCONNECTED_GROUP_ID";
  private static final String WAITING_ON_APPROVAL_GROUP_ID = "WAITING_ON_APPROVAL_GROUP_ID";
  private static final String TASK_ASSIGNED_GROUP_ID = "TASK_ASSIGNED_GROUP_ID";

  @Override
  public void save(BatchDelegateSelectionLog batch) {
    if (batch == null || batch.getDelegateSelectionLogs().isEmpty()) {
      return;
    }
    try {
      if (featureFlagService.isNotEnabled(
              DISABLE_DELEGATE_SELECTION_LOG, batch.getDelegateSelectionLogs().iterator().next().getAccountId())) {
        wingsPersistence.saveIgnoringDuplicateKeys(batch.getDelegateSelectionLogs());
        logger.info("Batch saved successfully");
      }
    } catch (Exception exception) {
      logger.error("Error while saving into Database ", exception);
    }
  }

  @Override
  public BatchDelegateSelectionLog createBatch(DelegateTask task) {
    if (task == null || task.getUuid() == null || !task.isSelectionLogsTrackingEnabled()) {
      return null;
    }
    return BatchDelegateSelectionLog.builder().taskId(task.getUuid()).build();
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

    batch.append(delegateSelectionLogBuilder.conclusion(ACCEPTED)
                     .message("Successfully matched required delegate capabilities")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(CAN_ASSIGN_GROUP_ID)
                     .build());
  }

  @Override
  public void logTaskAssigned(BatchDelegateSelectionLog batch, String accountId, String delegateId) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(SELECTED)
                     .message("Delegate assigned for task execution")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(TASK_ASSIGNED_GROUP_ID)
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

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("No matching include scope")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(NO_INCLUDE_SCOPE_MATCHED_GROUP_ID)
                     .build());
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

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("Matched exclude scope " + scope.getName())
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(EXCLUDE_SCOPE_MATCHED_GROUP_ID)
                     .build());
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

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("Missing selector " + selector)
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(MISSING_SELECTOR_GROUP_ID)
                     .build());
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

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("Missing all selectors")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(MISSING_ALL_SELECTORS_GROUP_ID)
                     .build());
  }

  @Override
  public List<DelegateSelectionLogParams> fetchTaskSelectionLogs(String accountId, String taskId) {
    List<DelegateSelectionLog> delegateSelectionLogsList = wingsPersistence.createQuery(DelegateSelectionLog.class)
                                                               .filter(DelegateSelectionLogKeys.accountId, accountId)
                                                               .filter(DelegateSelectionLogKeys.taskId, taskId)
                                                               .asList();

    List<DelegateSelectionLogParams> delegateSelectionLogs = new ArrayList<>();

    for (DelegateSelectionLog log : delegateSelectionLogsList) {
      delegateSelectionLogs.addAll(buildDelegateSelectionLogParamsList(log));
    }

    return delegateSelectionLogs;
  }

  private List<DelegateSelectionLogParams> buildDelegateSelectionLogParamsList(DelegateSelectionLog log) {
    List<DelegateSelectionLogParams> delegateSelectionLogParamsList = new ArrayList<>();

    for (String delegateId : log.getDelegateIds()) {
      Delegate delegate = delegateService.get(log.getAccountId(), delegateId, false);
      String delegateName = Optional.ofNullable(delegate).map(delegateService::obtainDelegateName).orElse(delegateId);
      String delegateHostName = Optional.ofNullable(delegate).map(Delegate::getHostName).orElse("");

      String delegateProfileName = "";
      if (delegate != null) {
        DelegateProfile delegateProfile = wingsPersistence.get(DelegateProfile.class, delegate.getDelegateProfileId());
        delegateProfileName = Optional.ofNullable(delegateProfile).map(DelegateProfile::getName).orElse("");
      }

      delegateSelectionLogParamsList.add(DelegateSelectionLogParams.builder()
                                             .delegateId(delegateId)
                                             .delegateName(delegateName)
                                             .delegateHostName(delegateHostName)
                                             .delegateProfileName(delegateProfileName)
                                             .conclusion(log.getConclusion())
                                             .message(log.getMessage())
                                             .eventTimestamp(log.getEventTimestamp())
                                             .build());
    }

    return delegateSelectionLogParamsList;
  }

  @Override
  public Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String taskId) {
    DelegateSelectionLog delegateSelectionLog = wingsPersistence.createQuery(DelegateSelectionLog.class)
                                                    .filter(DelegateSelectionLogKeys.taskId, taskId)
                                                    .filter(DelegateSelectionLogKeys.conclusion, SELECTED)
                                                    .get();
    if (delegateSelectionLog == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(buildDelegateSelectionLogParamsList(delegateSelectionLog).get(0));
  }

  @Override
  public void logDisconnectedDelegate(BatchDelegateSelectionLog batch, String accountId, Set<String> delegateIds) {
    if (batch == null) {
      return;
    }

    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(DISCONNECTED)
                     .message("Delegate was disconnected")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(DISCONNECTED_GROUP_ID)
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
                     .message("Delegate was waiting for approval")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(WAITING_ON_APPROVAL_GROUP_ID)
                     .build());
  }
}