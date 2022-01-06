/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.FeatureName;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogParams.DelegateSelectionLogParamsBuilder;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.delegate.beans.ProfileScopingRulesDetails;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogBuilder;
import io.harness.selection.log.DelegateSelectionLog.DelegateSelectionLogKeys;
import io.harness.selection.log.DelegateSelectionLogMetadata;
import io.harness.selection.log.DelegateSelectionLogTaskMetadata;
import io.harness.selection.log.ProfileScopingRulesMetadata;
import io.harness.service.intfc.DelegateCache;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("io.harness.beans.Cd1SetupFields")
@BreakDependencyOn("software.wings.beans.Application")
@BreakDependencyOn("software.wings.beans.Environment")
@BreakDependencyOn("software.wings.beans.Service")
@OwnedBy(DEL)
public class DelegateSelectionLogsServiceImpl implements DelegateSelectionLogsService {
  @Inject private HPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject private DelegateCache delegateCache;
  @Inject private FeatureFlagService featureFlagService;

  private static final String DISCONNECTED = "Disconnected";
  private static final String REJECTED = "Rejected";
  private static final String SELECTED = "Selected";
  private static final String ACCEPTED = "Accepted";
  private static final String INFO = "Info";

  private final LoadingCache<ImmutablePair<String, String>, String> setupAbstractionsCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new CacheLoader<ImmutablePair<String, String>, String>() {
            @Override
            public String load(@NotNull ImmutablePair<String, String> setupAbstractionKey) {
              return fetchSetupAbstractionName(setupAbstractionKey);
            }

            private String fetchSetupAbstractionName(ImmutablePair<String, String> setupAbstractionKey) {
              switch (setupAbstractionKey.getLeft()) {
                case Cd1SetupFields.APP_ID_FIELD:
                  Application app = persistence.get(Application.class, setupAbstractionKey.getRight());
                  return app != null ? app.getName() : setupAbstractionKey.getRight();
                case Cd1SetupFields.ENV_ID_FIELD:
                  Environment env = persistence.get(Environment.class, setupAbstractionKey.getRight());
                  return env != null ? env.getName() : setupAbstractionKey.getRight();
                case Cd1SetupFields.SERVICE_ID_FIELD:
                  Service service = persistence.get(Service.class, setupAbstractionKey.getRight());
                  return service != null ? service.getName() : setupAbstractionKey.getRight();
                default:
                  return setupAbstractionKey.getRight();
              }
            }
          });

  @Override
  public void save(BatchDelegateSelectionLog batch) {
    if (batch == null || isEmpty(batch.getDelegateSelectionLogs())) {
      return;
    }

    String accountId = batch.getDelegateSelectionLogs().get(0).getAccountId();
    if (featureFlagService.isEnabled(FeatureName.DELEGATE_SELECTION_LOGS_DISABLED, accountId)) {
      return;
    }

    try {
      persistence.saveIgnoringDuplicateKeys(batch.getDelegateSelectionLogs());
    } catch (Exception exception) {
      log.error("Error while saving into Database ", exception);
    }
  }

  @Override
  public BatchDelegateSelectionLog createBatch(DelegateTask task) {
    if (task == null || task.getUuid() == null || !task.isSelectionLogsTrackingEnabled()) {
      return null;
    }

    boolean isTaskNg =
        !isEmpty(task.getSetupAbstractions()) && Boolean.parseBoolean(task.getSetupAbstractions().get(NG));
    return BatchDelegateSelectionLog.builder().taskId(task.getUuid()).isTaskNg(isTaskNg).build();
  }

  @Override
  public void logTaskAssigned(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final String delegateId) {
    final String message = "Delegate assigned for task execution";
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, SELECTED);
  }

  @Override
  public void logNoIncludeScopeMatched(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final String delegateId) {
    final String message = "The delegate is not scoped to execute this task";
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, REJECTED);
  }

  @Override
  public void logExcludeScopeMatched(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      final String delegateId, final String scopeName) {
    final String message =
        String.format("Delegate is excluded to execute this task because of exclusion scope %s", scopeName);
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, REJECTED);
  }

  @Override
  public void logOwnerRuleNotMatched(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      Set<String> delegateIds, @Nullable final DelegateEntityOwner owner) {
    final String message = String.format("No matching owner: %s", owner != null ? owner.getIdentifier() : "null");
    logBatch(batch, accountId, delegateIds, message, REJECTED);
  }

  @Override
  public void logNoEligibleDelegatesToExecuteTask(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId) {
    final String message = "No eligible delegates in account to execute task";
    logBatch(batch, accountId, Sets.newHashSet(), message, REJECTED);
  }

  @Override
  public void logNoEligibleDelegatesAvailableToExecuteTask(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId) {
    final String message = "No eligible delegates in account available to execute task";
    logBatch(batch, accountId, Sets.newHashSet(), message, REJECTED);
  }

  @Override
  public void logEligibleDelegatesToExecuteTask(
      BatchDelegateSelectionLog batch, final Set<String> delegateIds, String accountId) {
    final String message = "Delegate eligible to execute task";
    logBatch(batch, accountId, delegateIds, message, INFO);
  }

  @Override
  public void logBroadcastToDelegate(BatchDelegateSelectionLog batch, Set<String> delegateIds, String accountId) {
    final String message = "Broadcasting to delegate";
    logBatch(batch, accountId, delegateIds, message, INFO);
  }

  @Override
  public void logProfileScopeRuleNotMatched(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      final String delegateId, final String delegateProfileId, final Set<String> scopingRulesDescriptions) {
    final String message = "Delegate profile scoping rules not matched";
    final DelegateSelectionLogMetadata metadata =
        DelegateSelectionLogMetadata.builder()
            .profileScopingRulesMetadata(ProfileScopingRulesMetadata.builder()
                                             .profileId(delegateProfileId)
                                             .scopingRulesDescriptions(scopingRulesDescriptions)
                                             .build())
            .build();

    final Map<String, DelegateSelectionLogMetadata> delegateMetadata = new HashMap<>();
    delegateMetadata.put(delegateId, metadata);

    logBatch(batch, accountId, Sets.newHashSet(delegateId), delegateMetadata, message, REJECTED);
  }

  @Override
  public void logMissingSelector(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final String delegateId) {
    final String message = "The delegate selector tags are not part of the task selector tags";
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, REJECTED);
  }

  @Override
  public void logDisconnectedDelegate(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final Set<String> delegateIds) {
    final String message = "Not broadcasting to delegate(s) since disconnected";
    logBatch(batch, accountId, delegateIds, message, INFO);
  }

  @Override
  public void logDisconnectedScalingGroup(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      final Set<String> disconnectedScalingGroup, final String groupName) {
    final String message = String.format("Delegate scaling group: %s was disconnected", groupName);
    logBatch(batch, accountId, disconnectedScalingGroup, message, DISCONNECTED);
  }

  @Override
  public void logMustExecuteOnDelegateMatched(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final String delegateId) {
    final String message = "Delegate was targeted for profile script execution";
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, ACCEPTED);
  }

  @Override
  public void logMustExecuteOnDelegateNotMatched(
      @Nullable final BatchDelegateSelectionLog batch, final String accountId, final String delegateId) {
    final String message = "Delegate was not targeted for profile script execution";
    logBatch(batch, accountId, Sets.newHashSet(delegateId), message, REJECTED);
  }

  @Override
  public List<DelegateSelectionLogParams> fetchTaskSelectionLogs(String accountId, String taskId) {
    List<DelegateSelectionLog> delegateSelectionLogsList = persistence.createQuery(DelegateSelectionLog.class)
                                                               .filter(DelegateSelectionLogKeys.accountId, accountId)
                                                               .filter(DelegateSelectionLogKeys.taskId, taskId)
                                                               .asList();

    List<DelegateSelectionLogParams> delegateSelectionLogs = new ArrayList<>();

    for (DelegateSelectionLog logObject : delegateSelectionLogsList) {
      if (logObject.getConclusion() != null && logObject.getConclusion().equals(INFO)) {
        delegateSelectionLogs.add(buildDelegateSelectionLogInfo(logObject));
      } else {
        delegateSelectionLogs.addAll(buildDelegateSelectionLogParamsList(logObject));
      }
    }

    return delegateSelectionLogs;
  }

  @Override
  public DelegateSelectionLogResponse fetchTaskSelectionLogsData(String accountId, String taskId) {
    List<DelegateSelectionLogParams> delegateSelectionLogParams = fetchTaskSelectionLogs(accountId, taskId);

    DelegateSelectionLogTaskMetadata taskMetadata = persistence.createQuery(DelegateSelectionLogTaskMetadata.class)
                                                        .filter(DelegateSelectionLogKeys.accountId, accountId)
                                                        .filter(DelegateSelectionLogKeys.taskId, taskId)
                                                        .get();

    Map<String, String> previewSetupAbstractions = new HashMap<>();
    if (taskMetadata != null && taskMetadata.getSetupAbstractions() != null) {
      previewSetupAbstractions =
          taskMetadata.getSetupAbstractions()
              .entrySet()
              .stream()
              .filter(map
                  -> Cd1SetupFields.APPLICATION.equals(map.getKey()) || Cd1SetupFields.SERVICE.equals(map.getKey())
                      || Cd1SetupFields.ENVIRONMENT.equals(map.getKey())
                      || Cd1SetupFields.ENVIRONMENT_TYPE.equals(map.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, map -> String.valueOf(map.getValue())));
    }

    return DelegateSelectionLogResponse.builder()
        .delegateSelectionLogs(delegateSelectionLogParams)
        .taskSetupAbstractions(previewSetupAbstractions)
        .build();
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

  @VisibleForTesting
  protected Map<String, String> processSetupAbstractions(Map<String, String> setupAbstractions) {
    if (setupAbstractions == null) {
      return null;
    }

    Map<String, String> mappedSetupAbstractions = new HashMap<>();

    for (Map.Entry<String, String> entry : setupAbstractions.entrySet()) {
      String setupAbstractionName = entry.getValue();

      if (isNotBlank(entry.getKey()) && isNotBlank(entry.getValue())) {
        try {
          setupAbstractionName = setupAbstractionsCache.get(ImmutablePair.of(entry.getKey(), entry.getValue()));
        } catch (ExecutionException e) {
          log.error("Unexpected exception occurred while processing setup abstractions.", e);
        }
      }

      mappedSetupAbstractions.put(
          Cd1SetupFields.mapSetupFieldKeyToHumanFriendlyName(entry.getKey()), setupAbstractionName);
    }

    return mappedSetupAbstractions;
  }

  private void logBatch(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      final Set<String> delegateIds, final String message, final String conclusion) {
    logBatch(batch, accountId, delegateIds, new HashMap<>(), message, conclusion);
  }

  private void logBatch(@Nullable final BatchDelegateSelectionLog batch, final String accountId,
      final Set<String> delegateIds, final Map<String, DelegateSelectionLogMetadata> metadata, final String message,
      final String conclusion) {
    if (batch == null) {
      log.debug("SelectionLog (no taskId): {}, Conclusion {}", message, conclusion);
      return;
    }

    final DelegateSelectionLogBuilder selectionLogBuilder =
        selectionLogBuilder(accountId, batch.getTaskId(), delegateIds)
            .conclusion(conclusion)
            .message(message)
            .eventTimestamp(System.currentTimeMillis());

    if (!metadata.isEmpty()) {
      selectionLogBuilder.delegateMetadata(metadata);
    }

    batch.append(selectionLogBuilder.build());
  }

  private DelegateSelectionLogBuilder selectionLogBuilder(
      final String accountId, final String taskId, final Set<String> delegateIds) {
    return DelegateSelectionLog.builder().accountId(accountId).taskId(taskId).delegateIds(delegateIds);
  }

  private DelegateSelectionLogParams buildDelegateSelectionLogInfo(DelegateSelectionLog selectionLog) {
    StringBuilder message = new StringBuilder(selectionLog.getMessage());
    if (!isEmpty(selectionLog.getDelegateIds())) {
      message.append(" {");
      for (String delegateId : selectionLog.getDelegateIds()) {
        Delegate delegate = delegateCache.get(selectionLog.getAccountId(), delegateId, false);
        String delegateName = Optional.ofNullable(delegate).map(delegateService::obtainDelegateName).orElse(delegateId);
        message.append(delegateName);
        message.append(", ");
      }
      message.append(" }");
    }
    return DelegateSelectionLogParams.builder()
        .conclusion(selectionLog.getConclusion())
        .message(message.toString())
        .eventTimestamp(selectionLog.getEventTimestamp())
        .build();
  }

  private List<DelegateSelectionLogParams> buildDelegateSelectionLogParamsList(DelegateSelectionLog selectionLog) {
    List<DelegateSelectionLogParams> delegateSelectionLogParamsList = new ArrayList<>();

    for (String delegateId : selectionLog.getDelegateIds()) {
      Delegate delegate = delegateCache.get(selectionLog.getAccountId(), delegateId, false);
      String delegateName = Optional.ofNullable(delegate).map(delegateService::obtainDelegateName).orElse(delegateId);
      String delegateHostName = Optional.ofNullable(delegate).map(Delegate::getHostName).orElse("");

      String delegateProfileName = "";
      String delegateType = "";
      if (delegate != null) {
        DelegateProfile delegateProfile = persistence.get(DelegateProfile.class, delegate.getDelegateProfileId());
        delegateProfileName = Optional.ofNullable(delegateProfile).map(DelegateProfile::getName).orElse("");
        delegateType = delegate.getDelegateType();
      }

      DelegateSelectionLogParamsBuilder delegateSelectionLogParamsBuilder =
          DelegateSelectionLogParams.builder()
              .delegateId(delegateId)
              .delegateType(delegateType)
              .delegateName(delegateName)
              .delegateHostName(delegateHostName)
              .delegateProfileName(delegateProfileName)
              .conclusion(selectionLog.getConclusion())
              .message(selectionLog.getMessage())
              .eventTimestamp(selectionLog.getEventTimestamp());

      if (selectionLog.getDelegateMetadata() != null && selectionLog.getDelegateMetadata().get(delegateId) != null
          && selectionLog.getDelegateMetadata().get(delegateId).getProfileScopingRulesMetadata() != null) {
        ProfileScopingRulesMetadata profileScopingRulesMetadata =
            selectionLog.getDelegateMetadata().get(delegateId).getProfileScopingRulesMetadata();

        DelegateProfile scopingRulesProfile =
            persistence.get(DelegateProfile.class, profileScopingRulesMetadata.getProfileId());

        delegateSelectionLogParamsBuilder.profileScopingRulesDetails(
            ProfileScopingRulesDetails.builder()
                .profileId(profileScopingRulesMetadata.getProfileId())
                .profileName(scopingRulesProfile != null ? scopingRulesProfile.getName() : null)
                .scopingRulesDescriptions(profileScopingRulesMetadata.getScopingRulesDescriptions())
                .build());
      }

      delegateSelectionLogParamsList.add(delegateSelectionLogParamsBuilder.build());
    }

    return delegateSelectionLogParamsList;
  }

  private String constructSelectionLogString(DelegateSelectionLog selectionLog) {
    return new StringBuilder()
        .append("Delegate selection log: delegates ")
        .append(selectionLog.getDelegateIds())
        .append(" for account: ")
        .append(selectionLog.getAccountId())
        .append(" and taskId: ")
        .append(String.join(" ", selectionLog.getTaskId(), selectionLog.getConclusion()))
        .append("with note: ")
        .append(selectionLog.getMessage())
        .append(" at ")
        .append(LocalDateTime.ofInstant(Instant.ofEpochMilli(selectionLog.getEventTimestamp()), ZoneId.systemDefault()))
        .toString();
  }
}
