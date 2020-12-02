package software.wings.service.impl;

import static io.harness.beans.FeatureName.DISABLE_DELEGATE_SELECTION_LOG;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.persistence.HPersistence;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogBuilder;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogKeys;

import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateSelectionLogsServiceImpl implements DelegateSelectionLogsService {
  @Inject private HPersistence persistence;
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
  private static final String PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID = "PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID";

  @Override
  public void save(BatchDelegateSelectionLog batch) {
    if (batch == null || batch.getDelegateSelectionLogs().isEmpty()) {
      return;
    }
    try {
      if (featureFlagService.isNotEnabled(
              DISABLE_DELEGATE_SELECTION_LOG, batch.getDelegateSelectionLogs().iterator().next().getAccountId())) {
        persistence.saveIgnoringDuplicateKeys(batch.getDelegateSelectionLogs());
        log.info("Batch saved successfully");
      } else {
        batch.getDelegateSelectionLogs()
            .stream()
            .map(selectionLog
                -> String.format(
                    "Delegate selection log: delegates %s for account: %s and taskId: %s %s with note: %s at: %s",
                    selectionLog.getDelegateIds(), selectionLog.getAccountId(), selectionLog.getTaskId(),
                    selectionLog.getConclusion(), selectionLog.getMessage(),
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(selectionLog.getEventTimestamp()), ZoneId.systemDefault())))
            .distinct()
            .forEach(log::info);
      }
    } catch (Exception exception) {
      log.error("Error while saving into Database ", exception);
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
  public void logProfileScopeRuleNotMatched(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, String scopingRuleDescription) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("Delegate profile scoping rule not matched: " + scopingRuleDescription)
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(PROFILE_SCOPE_RULE_NOT_MATCHED_GROUP_ID)
                     .build());
  }

  @Override
  public void logMissingSelector(
      BatchDelegateSelectionLog batch, String accountId, String delegateId, String selector, String selectorOrigin) {
    if (batch == null) {
      return;
    }

    Set<String> delegateIds = new HashSet<>();
    delegateIds.add(delegateId);
    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), delegateIds);

    batch.append(delegateSelectionLogBuilder.conclusion(REJECTED)
                     .message("The selector " + selector + " is configured in " + selectorOrigin
                         + ", but is not attached to this Delegate.")
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
    List<DelegateSelectionLog> delegateSelectionLogsList = persistence.createQuery(DelegateSelectionLog.class)
                                                               .filter(DelegateSelectionLogKeys.accountId, accountId)
                                                               .filter(DelegateSelectionLogKeys.taskId, taskId)
                                                               .asList();

    List<DelegateSelectionLogParams> delegateSelectionLogs = new ArrayList<>();

    for (DelegateSelectionLog logObject : delegateSelectionLogsList) {
      delegateSelectionLogs.addAll(buildDelegateSelectionLogParamsList(logObject));
    }

    return delegateSelectionLogs;
  }

  private List<DelegateSelectionLogParams> buildDelegateSelectionLogParamsList(DelegateSelectionLog selectionLog) {
    List<DelegateSelectionLogParams> delegateSelectionLogParamsList = new ArrayList<>();

    for (String delegateId : selectionLog.getDelegateIds()) {
      Delegate delegate = delegateService.get(selectionLog.getAccountId(), delegateId, false);
      String delegateName = Optional.ofNullable(delegate).map(delegateService::obtainDelegateName).orElse(delegateId);
      String delegateHostName = Optional.ofNullable(delegate).map(Delegate::getHostName).orElse("");

      String delegateProfileName = "";
      if (delegate != null) {
        DelegateProfile delegateProfile = persistence.get(DelegateProfile.class, delegate.getDelegateProfileId());
        delegateProfileName = Optional.ofNullable(delegateProfile).map(DelegateProfile::getName).orElse("");
      }

      delegateSelectionLogParamsList.add(DelegateSelectionLogParams.builder()
                                             .delegateId(delegateId)
                                             .delegateName(delegateName)
                                             .delegateHostName(delegateHostName)
                                             .delegateProfileName(delegateProfileName)
                                             .conclusion(selectionLog.getConclusion())
                                             .message(selectionLog.getMessage())
                                             .eventTimestamp(selectionLog.getEventTimestamp())
                                             .build());
    }

    return delegateSelectionLogParamsList;
  }

  @Override
  public Optional<DelegateSelectionLogParams> fetchSelectedDelegateForTask(String accountId, String taskId) {
    DelegateSelectionLog delegateSelectionLog = persistence.createQuery(DelegateSelectionLog.class)
                                                    .filter(DelegateSelectionLogKeys.accountId, accountId)
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

  @Override
  public void logDisconnectedScalingGroup(
      BatchDelegateSelectionLog batch, String accountId, Set<String> disconnectedScalingGroup, String groupName) {
    if (batch == null) {
      return;
    }

    DelegateSelectionLogBuilder delegateSelectionLogBuilder =
        retrieveDelegateSelectionLogBuilder(accountId, batch.getTaskId(), disconnectedScalingGroup);

    batch.append(delegateSelectionLogBuilder.conclusion(DISCONNECTED)
                     .message("Delegate scaling group: " + groupName + " was disconnected")
                     .eventTimestamp(System.currentTimeMillis())
                     .groupId(DISCONNECTED_GROUP_ID)
                     .build());
  }
}
