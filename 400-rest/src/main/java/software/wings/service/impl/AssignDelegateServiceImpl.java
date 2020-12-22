package software.wings.service.impl;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.task.TaskFailureReason.EXPIRED;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateActivity;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileScopingRule;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.TaskFailureReason;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.tasks.Cd1SetupFields;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateScope;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Created by brett on 7/20/17
 */
@Singleton
@Slf4j
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private static final SecureRandom random = new SecureRandom();
  public static final long MAX_DELEGATE_LAST_HEARTBEAT = (5 * 60 * 1000L) + (15 * 1000L); // 5 minutes 15 seconds

  public static final String ERROR_MESSAGE =
      "Delegate selection log: Delegate id: %s, Name: %s, Host name: %s, Profile name: %s, %s with note: %s at: %s";

  private static final long WHITELIST_TTL = TimeUnit.HOURS.toMillis(6);
  private static final long BLACKLIST_TTL = TimeUnit.MINUTES.toMillis(5);
  private static final long WHITELIST_REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(10);

  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private DelegateService delegateService;
  @Inject private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private Injector injector;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  private LoadingCache<ImmutablePair<String, String>, Optional<DelegateConnectionResult>>
      delegateConnectionResultCache =
          CacheBuilder.newBuilder()
              .maximumSize(10000)
              .expireAfterWrite(2, TimeUnit.MINUTES)
              .build(new CacheLoader<ImmutablePair<String, String>, Optional<DelegateConnectionResult>>() {
                @Override
                public Optional<DelegateConnectionResult> load(ImmutablePair<String, String> key) {
                  return Optional.ofNullable(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                 .filter(DelegateConnectionResultKeys.delegateId, key.getLeft())
                                                 .filter(DelegateConnectionResultKeys.criteria, key.getRight())
                                                 .get());
                }
              });

  private LoadingCache<String, List<Delegate>> accountDelegatesCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(MAX_DELEGATE_LAST_HEARTBEAT / 3, TimeUnit.MILLISECONDS)
          .build(new CacheLoader<String, List<Delegate>>() {
            @Override
            public List<Delegate> load(String accountId) {
              return wingsPersistence.createQuery(Delegate.class)
                  .filter(DelegateKeys.accountId, accountId)
                  .project(DelegateKeys.uuid, true)
                  .project(DelegateKeys.lastHeartBeat, true)
                  .project(DelegateKeys.status, true)
                  .project(DelegateKeys.delegateGroupName, true)
                  .asList();
            }
          });

  @Override
  public boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, DelegateTask task) {
    Delegate delegate = delegateService.get(task.getAccountId(), delegateId, false);
    if (delegate == null) {
      return false;
    }

    // Applicable only in case of targeting delegate for the purpose of delegate profile script execution
    if (isNotBlank(task.getMustExecuteOnDelegateId())) {
      if (delegateId.equals(task.getMustExecuteOnDelegateId())) {
        delegateSelectionLogsService.logMustExecuteOnDelegateMatched(batch, task.getAccountId(), delegateId);
        return true;
      } else {
        delegateSelectionLogsService.logMustExecuteOnDelegateNotMatched(batch, task.getAccountId(), delegateId);
        return false;
      }
    }

    boolean canAssign = canAssignDelegateScopes(batch, delegate, task)
        && canAssignDelegateProfileScopes(batch, delegate, task.getSetupAbstractions())
        && canAssignSelectors(batch, delegate, task.getExecutionCapabilities());

    if (canAssign) {
      delegateSelectionLogsService.logCanAssign(batch, task.getAccountId(), delegateId);
    }
    return canAssign;
  }

  public List<String> extractSelectors(DelegateTask task) {
    Set<String> selectors = new HashSet<>();

    if (isNotEmpty(task.getExecutionCapabilities())) {
      Set<String> selectorsCapability = task.getExecutionCapabilities()
                                            .stream()
                                            .filter(c -> c instanceof SelectorCapability)
                                            .flatMap(c -> ((SelectorCapability) c).getSelectors().stream())
                                            .collect(Collectors.toSet());
      selectors.addAll(selectorsCapability);
    }

    if (isNotEmpty(task.getTags())) {
      selectors.addAll(task.getTags());
    }

    return new ArrayList<>(selectors);
  }

  @Override
  public boolean canAssign(BatchDelegateSelectionLog batch, String delegateId, String accountId, String appId,
      String envId, String infraMappingId, TaskGroup taskGroup, List<ExecutionCapability> executionCapabilities,
      Map<String, String> taskSetupAbstractions) {
    Delegate delegate = delegateService.get(accountId, delegateId, false);
    if (delegate == null) {
      return false;
    }
    return canAssignDelegateScopes(batch, delegate, appId, envId, infraMappingId, taskGroup)
        && canAssignDelegateProfileScopes(batch, delegate, taskSetupAbstractions)
        && canAssignSelectors(batch, delegate, executionCapabilities);
  }

  private boolean canAssignDelegateScopes(BatchDelegateSelectionLog batch, Delegate delegate, DelegateTask task) {
    TaskGroup taskGroup =
        isNotBlank(task.getData().getTaskType()) ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup() : null;
    return canAssignDelegateScopes(
        batch, delegate, task.getAppId(), task.getEnvId(), task.getInfrastructureMappingId(), taskGroup);
  }

  @VisibleForTesting
  protected boolean canAssignDelegateProfileScopes(
      BatchDelegateSelectionLog batch, Delegate delegate, Map<String, String> taskSetupAbstractions) {
    DelegateProfile delegateProfile = wingsPersistence.get(DelegateProfile.class, delegate.getDelegateProfileId());
    if (delegateProfile == null) {
      log.warn(
          "Delegate profile {} not found. Considering this delegate profile matched", delegate.getDelegateProfileId());
      return true;
    }

    List<DelegateProfileScopingRule> delegateProfileScopingRules = delegateProfile.getScopingRules();

    if (isEmpty(delegateProfileScopingRules)) {
      return true;
    } else if (isEmpty(taskSetupAbstractions)) {
      log.warn(
          "No setup abstractions have been passed in from delegate task, while there are some profile scoping rules. Considering this delegate profile NOT matched");
      return false;
    }

    // All logs with logSequence,so as the map string builder should probably be removed before GA or changed to debug
    // level
    StringBuilder taskSetupAbstractionsPrintable = new StringBuilder();
    for (Map.Entry<String, String> entity : taskSetupAbstractions.entrySet()) {
      taskSetupAbstractionsPrintable.append(entity.getKey() + ":" + entity.getValue() + "; ");
    }
    String logSequence = batch != null && isNotBlank(batch.getTaskId()) ? batch.getTaskId() : generateUuid();
    log.info(logSequence + " - Starting profile scoping rules match with task abstractions {}.",
        taskSetupAbstractionsPrintable.toString());

    Set<String> failedRulesDescriptions = new HashSet<>();
    for (DelegateProfileScopingRule scopingRule : delegateProfileScopingRules) {
      boolean scopingRuleMatched = true;

      for (Map.Entry<String, Set<String>> entity : scopingRule.getScopingEntities().entrySet()) {
        String taskSetupAbstractionValue = taskSetupAbstractions.get(entity.getKey());

        if (isBlank(taskSetupAbstractionValue)
            || (entity.getValue() != null && !entity.getValue().contains(taskSetupAbstractionValue))) {
          // Temporary workaround, until all tasks start sending envType and serviceId
          boolean workaroundPassed =
              trySetupAbstractionsWorkaround(logSequence, taskSetupAbstractions, entity.getKey(), entity.getValue());

          if (!workaroundPassed) {
            log.info(
                logSequence + " - Scoping rule with description: {}, did not match with task abstractions for key {}.",
                scopingRule.getDescription(), entity.getKey());
            failedRulesDescriptions.add(scopingRule.getDescription());
            scopingRuleMatched = false;
            break;
          }
        }
      }

      if (scopingRuleMatched) {
        return true;
      }
    }

    delegateSelectionLogsService.logProfileScopeRuleNotMatched(
        batch, delegate.getAccountId(), delegate.getUuid(), delegateProfile.getUuid(), failedRulesDescriptions);

    return false;
  }

  private boolean trySetupAbstractionsWorkaround(String logSequence, Map<String, String> taskSetupAbstractions,
      String scopingEntityKey, Set<String> scopingEntityValues) {
    boolean workaroundPassed = false;

    if (Cd1SetupFields.ENV_TYPE_FIELD.equals(scopingEntityKey)) {
      if (taskSetupAbstractions.containsKey(Cd1SetupFields.ENV_ID_FIELD)) {
        Environment environment =
            wingsPersistence.get(Environment.class, taskSetupAbstractions.get(Cd1SetupFields.ENV_ID_FIELD));
        if (environment != null && environment.getEnvironmentType() != null) {
          workaroundPassed = scopingEntityValues.contains(environment.getEnvironmentType().name());
        }
      }
    }

    if (Cd1SetupFields.SERVICE_ID_FIELD.equals(scopingEntityKey)) {
      if (taskSetupAbstractions.containsKey(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD)) {
        InfrastructureMapping infrastructureMapping = wingsPersistence.get(
            InfrastructureMapping.class, taskSetupAbstractions.get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD));
        if (infrastructureMapping != null && isNotBlank(infrastructureMapping.getServiceId())) {
          workaroundPassed = scopingEntityValues.contains(infrastructureMapping.getServiceId());
        }
      }
    }

    log.warn(logSequence + " - Workaround for entity key {} passed: {}", scopingEntityKey, workaroundPassed);
    return workaroundPassed;
  }

  private boolean canAssignDelegateScopes(BatchDelegateSelectionLog batch, Delegate delegate, String appId,
      String envId, String infraMappingId, TaskGroup taskGroup) {
    List<DelegateScope> includeScopes = new ArrayList<>();

    if (isNotEmpty(delegate.getIncludeScopes())) {
      includeScopes = delegate.getIncludeScopes().stream().filter(Objects::nonNull).collect(toList());
    }

    boolean includeMatched = includeScopes.isEmpty();
    for (DelegateScope scope : includeScopes) {
      if (scopeMatch(scope, appId, envId, infraMappingId, taskGroup, delegate.getAccountId())) {
        includeMatched = true;
        break;
      }
    }

    if (!includeMatched) {
      delegateSelectionLogsService.logNoIncludeScopeMatched(batch, delegate.getAccountId(), delegate.getUuid());
      return false;
    }

    List<DelegateScope> excludeScopes = new ArrayList<>();
    if (isNotEmpty(delegate.getExcludeScopes())) {
      excludeScopes = delegate.getExcludeScopes().stream().filter(Objects::nonNull).collect(toList());
    }

    for (DelegateScope scope : excludeScopes) {
      if (scopeMatch(scope, appId, envId, infraMappingId, taskGroup, delegate.getAccountId())) {
        delegateSelectionLogsService.logExcludeScopeMatched(
            batch, delegate.getAccountId(), delegate.getUuid(), scope.getName());
        return false;
      }
    }

    return true;
  }

  private boolean canAssignSelectors(
      BatchDelegateSelectionLog batch, Delegate delegate, List<ExecutionCapability> executionCapabilities) {
    if (isEmpty(executionCapabilities)) {
      return true;
    }

    List<SelectorCapability> selectorsCapabilityList = executionCapabilities.stream()
                                                           .filter(c -> c instanceof SelectorCapability)
                                                           .map(c -> (SelectorCapability) c)
                                                           .collect(Collectors.toList());

    if (isEmpty(selectorsCapabilityList)) {
      return true;
    }

    Set<String> delegateSelectors = trimmedLowercaseSet(delegateService.retrieveDelegateSelectors(delegate));

    if (isEmpty(delegateSelectors)) {
      delegateSelectionLogsService.logMissingAllSelectors(batch, delegate.getAccountId(), delegate.getUuid());
      return false;
    }

    boolean canAssignSelector = true;

    for (SelectorCapability selectorCapability : selectorsCapabilityList) {
      Set<String> selectors = selectorCapability.getSelectors();
      String selectorOrigin = selectorCapability.getSelectorOrigin();
      for (String selector : trimmedLowercaseSet(selectors)) {
        if (!delegateSelectors.contains(selector)) {
          delegateSelectionLogsService.logMissingSelector(
              batch, delegate.getAccountId(), delegate.getUuid(), selector, selectorOrigin);
          canAssignSelector = false;
        }
      }
    }

    return canAssignSelector;
  }

  private boolean scopeMatch(
      DelegateScope scope, String appId, String envId, String infraMappingId, TaskGroup taskGroup, String accountId) {
    if (!scope.isValid()) {
      log.error("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate scope cannot be empty.");
    }
    boolean match = true;

    if (isNotEmpty(scope.getEnvironmentTypes())) {
      if (isNotBlank(appId) && isNotBlank(envId)) {
        Environment environment = environmentService.get(appId, envId, false);
        if (environment == null) {
          log.info("Environment {} referenced by scope {} does not exist.", envId, scope.getName());
        }
        match = environment != null && scope.getEnvironmentTypes().contains(environment.getEnvironmentType());
      } else {
        match = false;
      }
    }
    if (match && isNotEmpty(scope.getTaskTypes())) {
      match = scope.getTaskTypes().contains(taskGroup);
    }
    if (match && isNotEmpty(scope.getApplications())) {
      match = isNotBlank(appId) && scope.getApplications().contains(appId);
    }
    if (match && isNotEmpty(scope.getEnvironments())) {
      match = isNotBlank(envId) && scope.getEnvironments().contains(envId);
    }

    if (isNotEmpty(scope.getInfrastructureDefinitions()) || isNotEmpty(scope.getServices())) {
      InfrastructureMapping infrastructureMapping =
          isNotBlank(infraMappingId) ? infrastructureMappingService.get(appId, infraMappingId) : null;
      if (infrastructureMapping != null) {
        if (match && isNotEmpty(scope.getInfrastructureDefinitions())) {
          match = scope.getInfrastructureDefinitions().contains(infrastructureMapping.getInfrastructureDefinitionId());
        }
        if (match && isNotEmpty(scope.getServices())) {
          match = scope.getServices().contains(infrastructureMapping.getServiceId());
        }
      } else {
        match = false;
      }

    } else {
      if (match && isNotEmpty(scope.getServiceInfrastructures())) {
        match = isNotBlank(infraMappingId) && scope.getServiceInfrastructures().contains(infraMappingId);
      }
    }

    return match;
  }

  @Override
  public boolean isWhitelisted(DelegateTask task, String delegateId) {
    try {
      boolean matching = true;
      for (String criteria : fetchCriteria(task)) {
        if (isNotBlank(criteria)) {
          Optional<DelegateConnectionResult> result =
              delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criteria));
          if (!result.isPresent() || result.get().getLastUpdatedAt() < clock.millis() - WHITELIST_TTL
              || !result.get().isValidated()) {
            matching = false;
            break;
          }
        }
      }
      return matching;
    } catch (Exception e) {
      log.error("Error checking whether delegate is whitelisted for task {}", task.getUuid(), e);
    }
    return false;
  }

  @Override
  public boolean shouldValidate(DelegateTask task, String delegateId) {
    try {
      for (String criteria : fetchCriteria(task)) {
        if (isNotBlank(criteria)) {
          Optional<DelegateConnectionResult> result =
              delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criteria));
          if (!result.isPresent() || !result.get().isValidated()
              || clock.millis() - result.get().getLastUpdatedAt() > BLACKLIST_TTL
              || (!retrieveActiveDelegates(task.getAccountId(), null).contains(delegateId)
                  && isEmpty(connectedWhitelistedDelegates(task)))) {
            return true;
          }
        } else {
          return true;
        }
      }
    } catch (Exception e) {
      log.error("Error checking whether delegate should validate task {}", task.getUuid(), e);
    }
    return false;
  }

  @Override
  public List<String> connectedWhitelistedDelegates(DelegateTask task) {
    List<String> delegateIds = new ArrayList<>();
    try {
      BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);

      List<String> connectedEligibleDelegates = retrieveActiveDelegates(task.getAccountId(), batch)
                                                    .stream()
                                                    .filter(delegateId -> canAssign(batch, delegateId, task))
                                                    .collect(toList());

      delegateSelectionLogsService.save(batch);

      List<String> criteria = fetchCriteria(task);
      if (isEmpty(criteria)) {
        return connectedEligibleDelegates;
      }

      for (String delegateId : connectedEligibleDelegates) {
        boolean matching = true;
        for (String criterion : criteria) {
          Optional<DelegateConnectionResult> result =
              delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criterion));
          if (!result.isPresent() || !result.get().isValidated()) {
            matching = false;
            break;
          }
        }
        if (matching) {
          delegateIds.add(delegateId);
        }
      }
    } catch (Exception e) {
      log.error("Error checking for whitelisted delegates", e);
    }
    return delegateIds;
  }

  protected List<String> fetchCriteria(DelegateTask task) {
    if (isEmpty(task.getExecutionCapabilities())) {
      return emptyList();
    }

    return task.getExecutionCapabilities()
        .stream()
        .filter(e -> e.evaluationMode() == ExecutionCapability.EvaluationMode.AGENT)
        .map(ExecutionCapability::fetchCapabilityBasis)
        .collect(toList());
  }

  @Override
  public String pickFirstAttemptDelegate(DelegateTask task) {
    List<String> delegates = connectedWhitelistedDelegates(task);
    if (delegates.isEmpty()) {
      log.info("No first attempt delegate was picked");
      return null;
    }
    return delegates.get(random.nextInt(delegates.size()));
  }

  private static final FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions();

  @Override
  public void refreshWhitelist(DelegateTask task, String delegateId) {
    try {
      for (String criteria : fetchCriteria(task)) {
        if (isNotBlank(criteria)) {
          Optional<DelegateConnectionResult> cachedResult =
              delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criteria));
          if (cachedResult.isPresent()
              && cachedResult.get().getLastUpdatedAt() < clock.millis() - WHITELIST_REFRESH_INTERVAL) {
            Query<DelegateConnectionResult> query =
                wingsPersistence.createQuery(DelegateConnectionResult.class)
                    .filter(DelegateConnectionResultKeys.accountId, task.getAccountId())
                    .filter(DelegateConnectionResultKeys.delegateId, delegateId)
                    .filter(DelegateConnectionResultKeys.criteria, criteria);
            UpdateOperations<DelegateConnectionResult> updateOperations =
                wingsPersistence.createUpdateOperations(DelegateConnectionResult.class)
                    .set(DelegateConnectionResultKeys.lastUpdatedAt, clock.millis())
                    .set(DelegateConnectionResultKeys.validUntil, DelegateConnectionResult.getValidUntilTime());
            DelegateConnectionResult result =
                wingsPersistence.findAndModify(query, updateOperations, findAndModifyOptions);
            if (result != null) {
              log.info("Whitelist entry refreshed");
            } else {
              log.info("Whitelist entry was not updated");
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Error refreshing whitelist entry for task {}", task.getUuid(), e);
    }
  }

  @Override
  public void saveConnectionResults(List<DelegateConnectionResult> results) {
    List<DelegateConnectionResult> resultsToSave =
        results.stream().filter(result -> isNotBlank(result.getCriteria())).collect(toList());

    for (DelegateConnectionResult result : resultsToSave) {
      Key<DelegateConnectionResult> existingResultKey =
          wingsPersistence.createQuery(DelegateConnectionResult.class)
              .filter(DelegateConnectionResultKeys.accountId, result.getAccountId())
              .filter(DelegateConnectionResultKeys.delegateId, result.getDelegateId())
              .filter(DelegateConnectionResultKeys.criteria, result.getCriteria())
              .getKey();
      if (existingResultKey != null) {
        wingsPersistence.updateField(
            DelegateConnectionResult.class, existingResultKey.getId().toString(), "validated", result.isValidated());
      } else {
        try {
          wingsPersistence.save(result);
        } catch (DuplicateKeyException e) {
          log.warn("Result has already been saved. ", e);
        }
      }
    }
  }

  @Override
  public void clearConnectionResults(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                .filter(DelegateConnectionResultKeys.accountId, accountId));
  }

  @Override
  public void clearConnectionResults(String accountId, String delegateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                .filter(DelegateConnectionResultKeys.accountId, accountId)
                                .filter(DelegateConnectionResultKeys.delegateId, delegateId));
  }

  @Override
  public String getActiveDelegateAssignmentErrorMessage(TaskFailureReason reason, DelegateTask delegateTask) {
    log.info("Delegate task is terminated");

    String errorMessage = "Unknown";

    List<DelegateSelectionLogParams> delegateSelectionLogs =
        delegateSelectionLogsService.fetchTaskSelectionLogs(delegateTask.getAccountId(), delegateTask.getUuid());

    if (reason != EXPIRED && isNotEmpty(delegateSelectionLogs)) {
      return delegateSelectionLogs.stream()
          .map(selectionLog
              -> String.format(String.format(ERROR_MESSAGE, selectionLog.getDelegateId(),
                  selectionLog.getDelegateName(), selectionLog.getDelegateHostName(),
                  selectionLog.getDelegateProfileName(), selectionLog.getConclusion(), selectionLog.getMessage(),
                  LocalDateTime.ofInstant(
                      Instant.ofEpochMilli(selectionLog.getEventTimestamp()), ZoneId.systemDefault()))))
          .distinct()
          .collect(Collectors.joining(", "));
    }

    try {
      List<String> activeDelegates = retrieveActiveDelegates(delegateTask.getAccountId(), null);

      log.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

      List<String> whitelistedDelegates = connectedWhitelistedDelegates(delegateTask);
      if (activeDelegates.isEmpty()) {
        errorMessage = "There were no active delegates to complete the task.";
      } else if (whitelistedDelegates.isEmpty()) {
        StringBuilder msg = new StringBuilder();
        for (String delegateId : activeDelegates) {
          Delegate delegate = delegateService.get(delegateTask.getAccountId(), delegateId, false);
          if (delegate != null) {
            msg.append(" ===> ").append(delegate.getHostName()).append(": ");
            boolean canAssignScope = canAssignDelegateScopes(null, delegate, delegateTask);
            boolean canAssignTags = canAssignSelectors(null, delegate, delegateTask.getExecutionCapabilities());
            if (!canAssignScope) {
              msg.append("Not in scope");
            }
            if (!canAssignScope && !canAssignTags) {
              msg.append(" - ");
            }
            if (!canAssignTags) {
              msg.append("Tag mismatch: ").append(Optional.ofNullable(delegate.getTags()).orElse(emptyList()));
            }
            if (canAssignScope && canAssignTags) {
              msg.append("In scope and no tag mismatch");
            }
            msg.append('\n');
          }
        }
        String taskTagsMsg = isNotEmpty(delegateTask.getTags()) ? " Task tags: " + delegateTask.getTags() : "";
        errorMessage =
            "None of the active delegates were eligible to complete the task." + taskTagsMsg + "\n\n" + msg.toString();
      } else if (delegateTask.getDelegateId() != null) {
        Delegate delegate = delegateService.get(delegateTask.getAccountId(), delegateTask.getDelegateId(), false);
        errorMessage = "Delegate task timed out. Delegate: "
            + (delegate != null ? delegate.getHostName() : "not found: " + delegateTask.getDelegateId());
      } else {
        errorMessage = "Delegate task was never assigned and timed out.";
      }
    } catch (Exception e) {
      log.error("Execution exception", e);
    }
    return errorMessage;
  }

  @Override
  public boolean noInstalledDelegates(String accountId) {
    try {
      return accountDelegatesCache.get(accountId).isEmpty();
    } catch (ExecutionException ex) {
      log.error("Unexpected error occurred while fetching delegates from cache.", ex);
      return true;
    }
  }

  @Override
  public List<String> retrieveActiveDelegates(String accountId, BatchDelegateSelectionLog batch) {
    try {
      List<Delegate> accountDelegates = accountDelegatesCache.get(accountId);
      if (accountDelegates.isEmpty()) {
        /* Cache invalidation was added here in order to cover the edge case, when there are no delegates in db for
         * the given account, so that the cache has an opportunity to refresh on a next invocation, instead of waiting
         * for the whole cache validity period to pass and returning empty list.
         * */
        accountDelegatesCache.invalidate(accountId);
      }

      return identifyActiveDelegateIds(accountDelegates, accountId, batch);
    } catch (ExecutionException ex) {
      log.error("Unexpected error occurred while fetching delegates from cache.", ex);
      return emptyList();
    }
  }

  private List<String> identifyActiveDelegateIds(
      List<Delegate> accountDelegates, String accountId, BatchDelegateSelectionLog batch) {
    long oldestAcceptableHeartBeat = clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT;

    Map<DelegateActivity, List<Delegate>> delegatesMap =
        accountDelegates.stream().collect(Collectors.groupingBy(delegate -> {
          if (Status.ENABLED == delegate.getStatus()) {
            if (delegate.getLastHeartBeat() > oldestAcceptableHeartBeat) {
              return DelegateActivity.ACTIVE;
            } else {
              return DelegateActivity.DISCONNECTED;
            }
          } else if (Status.WAITING_FOR_APPROVAL == delegate.getStatus()) {
            return DelegateActivity.WAITING_FOR_APPROVAL;
          }
          return DelegateActivity.OTHER;
        }, Collectors.toList()));

    logInactiveDelegates(batch, accountId, delegatesMap);

    return delegatesMap.get(DelegateActivity.ACTIVE) == null
        ? emptyList()
        : delegatesMap.get(DelegateActivity.ACTIVE).stream().map(Delegate::getUuid).collect(Collectors.toList());
  }

  @VisibleForTesting
  protected void logInactiveDelegates(
      BatchDelegateSelectionLog batch, String accountId, Map<DelegateActivity, List<Delegate>> delegatesMap) {
    if (batch == null) {
      return;
    }

    if (delegatesMap.get(DelegateActivity.DISCONNECTED) != null) {
      Set<String> disconnectedDelegateIds = delegatesMap.get(DelegateActivity.DISCONNECTED)
                                                .stream()
                                                .filter(a -> isEmpty(a.getDelegateGroupName()))
                                                .map(Delegate::getUuid)
                                                .collect(Collectors.toSet());
      if (isNotEmpty(disconnectedDelegateIds)) {
        delegateSelectionLogsService.logDisconnectedDelegate(batch, accountId, disconnectedDelegateIds);
      }
    }

    Set<String> disconnectedScalingGroup = new HashSet<>();
    if (delegatesMap.get(DelegateActivity.DISCONNECTED) != null) {
      disconnectedScalingGroup = delegatesMap.get(DelegateActivity.DISCONNECTED)
                                     .stream()
                                     .filter(a -> isNotEmpty(a.getDelegateGroupName()))
                                     .map(Delegate::getDelegateGroupName)
                                     .collect(Collectors.toSet());
    }

    Set<String> connectedScalingGroup = new HashSet<>();
    if (delegatesMap.get(DelegateActivity.ACTIVE) != null) {
      connectedScalingGroup = delegatesMap.get(DelegateActivity.ACTIVE)
                                  .stream()
                                  .filter(a -> isNotEmpty(a.getDelegateGroupName()))
                                  .map(Delegate::getDelegateGroupName)
                                  .collect(Collectors.toSet());
    }

    disconnectedScalingGroup.removeAll(connectedScalingGroup);

    for (String groupName : disconnectedScalingGroup) {
      delegateSelectionLogsService.logDisconnectedScalingGroup(batch, accountId, disconnectedScalingGroup, groupName);
    }

    if (delegatesMap.get(DelegateActivity.WAITING_FOR_APPROVAL) != null) {
      Set<String> wapprDelegateIds = delegatesMap.get(DelegateActivity.WAITING_FOR_APPROVAL)
                                         .stream()
                                         .map(Delegate::getUuid)
                                         .collect(Collectors.toSet());
      delegateSelectionLogsService.logWaitingForApprovalDelegate(batch, accountId, wapprDelegateIds);
    }
  }
}
