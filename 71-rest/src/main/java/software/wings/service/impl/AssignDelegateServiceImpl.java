package software.wings.service.impl;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.FeatureName.DELEGATE_TAGS_EXTENDED;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.DelegateTask;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.DelegateScope;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 7/20/17
 */
@Singleton
@Slf4j
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private static final SecureRandom random = new SecureRandom();
  public static final long MAX_DELEGATE_LAST_HEARTBEAT = (5 * 60 * 1000L) + (15 * 1000L); // 5 minutes 15 seconds

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

  private LoadingCache<String, List<String>> accountConnectedDelegatesCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, List<String>>() {
            @Override
            public List<String> load(String accountId) {
              return wingsPersistence.createQuery(Delegate.class)
                  .filter(DelegateKeys.accountId, accountId)
                  .field("lastHeartBeat")
                  .greaterThan(clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT)
                  .asKeyList()
                  .stream()
                  .map(key -> key.getId().toString())
                  .collect(toList());
            }
          });

  @Override
  public boolean canAssign(String delegateId, DelegateTask task) {
    Delegate delegate = delegateService.get(task.getAccountId(), delegateId, false);
    if (delegate == null) {
      return false;
    }
    return canAssignScopes(delegate, task) && canAssignSelectors(delegate, task.getTags());
  }

  @Override
  public boolean canAssign(String delegateId, String accountId, String appId, String envId, String infraMappingId,
      TaskGroup taskGroup, List<String> tags) {
    Delegate delegate = delegateService.get(accountId, delegateId, false);
    if (delegate == null) {
      return false;
    }
    return canAssignScopes(delegate, appId, envId, infraMappingId, taskGroup) && canAssignSelectors(delegate, tags);
  }

  private boolean canAssignScopes(Delegate delegate, DelegateTask task) {
    TaskGroup taskGroup =
        isNotBlank(task.getData().getTaskType()) ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup() : null;
    return canAssignScopes(delegate, task.getAppId(), task.getEnvId(), task.getInfrastructureMappingId(), taskGroup);
  }

  private boolean canAssignScopes(
      Delegate delegate, String appId, String envId, String infraMappingId, TaskGroup taskGroup) {
    List<DelegateScope> includeScopes = new ArrayList<>();

    if (isNotEmpty(delegate.getIncludeScopes())) {
      includeScopes = delegate.getIncludeScopes().stream().filter(Objects::nonNull).collect(toList());
    }

    boolean includeMatched = includeScopes.isEmpty();
    for (DelegateScope scope : includeScopes) {
      if (scopeMatch(scope, appId, envId, infraMappingId, taskGroup, delegate.getAccountId())) {
        delegateSelectionLogsService.logIncludeScopeMatched(scope, delegate.getUuid());
        includeMatched = true;
        break;
      }
    }

    if (!includeMatched) {
      delegateSelectionLogsService.logNoIncludeScopeMatched(delegate.getUuid());
      return false;
    }

    List<DelegateScope> excludeScopes = new ArrayList<>();
    if (isNotEmpty(delegate.getExcludeScopes())) {
      excludeScopes = delegate.getExcludeScopes().stream().filter(Objects::nonNull).collect(toList());
    }

    for (DelegateScope scope : excludeScopes) {
      if (scopeMatch(scope, appId, envId, infraMappingId, taskGroup, delegate.getAccountId())) {
        delegateSelectionLogsService.logExcludeScopeMatched(scope, delegate.getUuid());
        return false;
      }
    }

    return true;
  }

  private boolean canAssignSelectors(Delegate delegate, List<String> tags) {
    if (isEmpty(tags)) {
      return true;
    }

    Set<String> selectors = delegate.getTags() == null ? new HashSet<>() : trimmedLowercaseSet(delegate.getTags());
    if (featureFlagService.isEnabled(DELEGATE_TAGS_EXTENDED, delegate.getAccountId())) {
      if (delegate.getHostName() != null) {
        selectors.add(delegate.getHostName().trim().toLowerCase());
      }

      if (delegate.getDelegateName() != null) {
        selectors.add(delegate.getDelegateName().trim().toLowerCase());
      }
    }

    if (isEmpty(selectors)) {
      delegateSelectionLogsService.logMissingAllSelectors(delegate.getUuid());
      return false;
    }

    boolean canAssignSelector = true;
    for (String selector : trimmedLowercaseSet(tags)) {
      if (!selectors.contains(selector)) {
        delegateSelectionLogsService.logMissingSelector(selector, delegate.getUuid());
        canAssignSelector = false;
      }
    }

    return canAssignSelector;
  }

  private boolean scopeMatch(
      DelegateScope scope, String appId, String envId, String infraMappingId, TaskGroup taskGroup, String accountId) {
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);
    if (!scope.isValid()) {
      logger.error("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate scope cannot be empty.");
    }
    boolean match = true;

    if (isNotEmpty(scope.getEnvironmentTypes())) {
      if (isNotBlank(appId) && isNotBlank(envId)) {
        Environment environment = environmentService.get(appId, envId, false);
        if (environment == null) {
          logger.info("Environment {} referenced by scope {} does not exist.", envId, scope.getName());
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

    if (infraRefactor && (isNotEmpty(scope.getInfrastructureDefinitions()) || isNotEmpty(scope.getServices()))) {
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
      for (String criteria : fetchCriteria(task)) {
        if (isNotBlank(criteria)) {
          Optional<DelegateConnectionResult> result =
              delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criteria));
          if (result.isPresent() && result.get().getLastUpdatedAt() > clock.millis() - WHITELIST_TTL
              && result.get().isValidated()) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error checking whether delegate is whitelisted for task {}", task.getUuid(), e);
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
          if (!result.isPresent() || result.get().isValidated()
              || clock.millis() - result.get().getLastUpdatedAt() > BLACKLIST_TTL
              || isEmpty(connectedWhitelistedDelegates(task))) {
            return true;
          }
        } else {
          return true;
        }
      }
    } catch (Exception e) {
      logger.error("Error checking whether delegate should validate task {}", task.getUuid(), e);
    }
    return false;
  }

  @Override
  public List<String> connectedWhitelistedDelegates(DelegateTask task) {
    List<String> delegateIds = new ArrayList<>();
    try {
      List<String> connectedEligibleDelegates = accountConnectedDelegatesCache.get(task.getAccountId())
                                                    .stream()
                                                    .filter(delegateId -> canAssign(delegateId, task))
                                                    .collect(toList());

      for (String criteria : fetchCriteria(task)) {
        if (isNotBlank(criteria)) {
          for (String delegateId : connectedEligibleDelegates) {
            Optional<DelegateConnectionResult> result =
                delegateConnectionResultCache.get(ImmutablePair.of(delegateId, criteria));
            if (result.isPresent() && result.get().isValidated()) {
              delegateIds.add(delegateId);
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error checking for whitelisted delegates", e);
    }
    return delegateIds;
  }

  private List<String> fetchCriteria(DelegateTask task) {
    // TODO: For now always use the original criteria
    return TaskType.valueOf(task.getData().getTaskType()).getCriteria(task, injector);
  }

  @Override
  public String pickFirstAttemptDelegate(DelegateTask task) {
    List<String> delegates = connectedWhitelistedDelegates(task);
    if (delegates.isEmpty()) {
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
              logger.info("Whitelist entry refreshed");
            } else {
              logger.info("Whitelist entry was not updated");
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error refreshing whitelist entry for task {}", task.getUuid(), e);
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
          logger.warn("Result has already been saved. ", e);
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
  public String getActiveDelegateAssignmentErrorMessage(DelegateTask delegateTask) {
    logger.info("Delegate task is terminated");

    String errorMessage = "Unknown";

    try {
      List<String> activeDelegates = accountConnectedDelegatesCache.get(delegateTask.getAccountId());

      logger.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

      List<String> whitelistedDelegates = connectedWhitelistedDelegates(delegateTask);

      if (activeDelegates.isEmpty()) {
        errorMessage = "There were no active delegates to complete the task.";
      } else if (whitelistedDelegates.isEmpty()) {
        StringBuilder msg = new StringBuilder();
        for (String delegateId : activeDelegates) {
          Delegate delegate = delegateService.get(delegateTask.getAccountId(), delegateId, false);
          if (delegate != null) {
            msg.append(" ===> ").append(delegate.getHostName()).append(": ");
            boolean canAssignScope = canAssignScopes(delegate, delegateTask);
            boolean canAssignTags = canAssignSelectors(delegate, delegateTask.getTags());
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
      logger.error("Execution exception", e);
    }
    return errorMessage;
  }
}
