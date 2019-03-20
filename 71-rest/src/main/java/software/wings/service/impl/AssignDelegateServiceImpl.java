package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.CapabilityUtil.isTaskTypeMigratedToCapabilityFramework;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.FeatureName;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 7/20/17
 */
@Singleton
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(AssignDelegateServiceImpl.class);

  private static final long WHITELIST_TTL = TimeUnit.HOURS.toMillis(6);
  private static final long BLACKLIST_TTL = TimeUnit.MINUTES.toMillis(5);
  private static final long WHITELIST_REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(10);

  @Inject private DelegateService delegateService;
  @Inject private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private Injector injector;
  @Inject private FeatureFlagService featureFlagService;

  private LoadingCache<ImmutablePair<String, String>, Optional<DelegateConnectionResult>>
      delegateConnectionResultCache =
          CacheBuilder.newBuilder()
              .maximumSize(10000)
              .expireAfterWrite(2, TimeUnit.MINUTES)
              .build(new CacheLoader<ImmutablePair<String, String>, Optional<DelegateConnectionResult>>() {
                public Optional<DelegateConnectionResult> load(ImmutablePair<String, String> key) {
                  return Optional.ofNullable(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                 .filter("delegateId", key.getLeft())
                                                 .filter("criteria", key.getRight())
                                                 .get());
                }
              });

  private LoadingCache<String, List<String>> accountConnectedDelegatesCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, List<String>>() {
            public List<String> load(String accountId) {
              return wingsPersistence.createQuery(Delegate.class)
                  .filter("accountId", accountId)
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
    return canAssign(delegateId, task.getAccountId(), task.getAppId(), task.getEnvId(),
        task.getInfrastructureMappingId(),
        isNotBlank(task.getData().getTaskType()) ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup() : null,
        task.getTags());
  }

  @Override
  public boolean canAssign(String delegateId, String accountId, String appId, String envId, String infraMappingId,
      TaskGroup taskGroup, List<String> tags) {
    Delegate delegate = delegateService.get(accountId, delegateId, false);
    if (delegate == null) {
      return false;
    }
    return (isEmpty(delegate.getIncludeScopes())
               || delegate.getIncludeScopes().stream().anyMatch(
                      scope -> scopeMatch(scope, appId, envId, infraMappingId, taskGroup)))
        && (isEmpty(delegate.getExcludeScopes())
               || delegate.getExcludeScopes().stream().noneMatch(
                      scope -> scopeMatch(scope, appId, envId, infraMappingId, taskGroup)))
        && (isEmpty(tags) || (isNotEmpty(delegate.getTags()) && delegate.getTags().containsAll(tags)));
  }

  private boolean canAssignTags(Delegate delegate, DelegateTask task) {
    return isEmpty(task.getTags())
        || (isNotEmpty(delegate.getTags()) && delegate.getTags().containsAll(task.getTags()));
  }

  private boolean canAssignScopes(Delegate delegate, DelegateTask task) {
    return (isEmpty(delegate.getIncludeScopes())
               || delegate.getIncludeScopes().stream().anyMatch(scope
                      -> scopeMatch(scope, task.getAppId(), task.getEnvId(), task.getInfrastructureMappingId(),
                          isNotBlank(task.getData().getTaskType())
                              ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup()
                              : null)))
        && (isEmpty(delegate.getExcludeScopes())
               || delegate.getExcludeScopes().stream().noneMatch(scope
                      -> scopeMatch(scope, task.getAppId(), task.getEnvId(), task.getInfrastructureMappingId(),
                          isNotBlank(task.getData().getTaskType())
                              ? TaskType.valueOf(task.getData().getTaskType()).getTaskGroup()
                              : null)));
  }

  private boolean scopeMatch(
      DelegateScope scope, String appId, String envId, String infraMappingId, TaskGroup taskGroup) {
    if (!scope.isValid()) {
      logger.error("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Delegate scope cannot be empty.");
    }
    boolean match = true;

    if (isNotEmpty(scope.getEnvironmentTypes())) {
      match = isNotBlank(appId) && isNotBlank(envId)
          && scope.getEnvironmentTypes().contains(environmentService.get(appId, envId, false).getEnvironmentType());
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
    if (match && isNotEmpty(scope.getServiceInfrastructures())) {
      match = isNotBlank(infraMappingId) && scope.getServiceInfrastructures().contains(infraMappingId);
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
      logger.error(format("Error checking whether delegate is whitelisted for task %s", task.getUuid()), e);
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
      logger.error(format("Error checking whether delegate should validate task %s", task.getUuid()), e);
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
      logger.error(format("Error checking for whitelisted delegates for task %s", task.getUuid()), e);
    }
    return delegateIds;
  }

  // TODO: This is temporary solution till all DelegateValidationTasks are moved to
  // TODO: New Capability Framework. This will simplify once that is done
  private List<String> fetchCriteria(DelegateTask task) {
    // Capability Generation has already happened.
    if (isNotEmpty(task.getExecutionCapabilities())) {
      return task.getExecutionCapabilities()
          .stream()
          .map(executionCapability -> executionCapability.fetchCapabilityBasis())
          .collect(toList());
    }

    if (isTaskTypeMigratedToCapabilityFramework(task.getData().getTaskType())
        && featureFlagService.isEnabled(FeatureName.DELEGATE_CAPABILITY_FRAMEWORK, task.getAccountId())) {
      return TaskType.valueOf(task.getData().getTaskType()).getCriteriaVersionForCapabilityFramework(task, injector);
    }
    return TaskType.valueOf(task.getData().getTaskType()).getCriteria(task, injector);
  }

  @Override
  public String pickFirstAttemptDelegate(DelegateTask task) {
    List<String> delegates = connectedWhitelistedDelegates(task);
    if (delegates.isEmpty()) {
      return null;
    }
    return delegates.get(new Random().nextInt(delegates.size()));
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
            Query<DelegateConnectionResult> query = wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                        .filter("accountId", task.getAccountId())
                                                        .filter("delegateId", delegateId)
                                                        .filter("criteria", criteria);
            UpdateOperations<DelegateConnectionResult> updateOperations =
                wingsPersistence.createUpdateOperations(DelegateConnectionResult.class)
                    .set(DelegateConnectionResult.LAST_UPDATED_AT_KEY, clock.millis());
            DelegateConnectionResult result =
                wingsPersistence.findAndModify(query, updateOperations, findAndModifyOptions);
            if (result != null) {
              logger.info("Whitelist entry refreshed for task {} and delegate {}", task.getUuid(), delegateId);
            } else {
              logger.info("Whitelist entry was not updated for task {} and delegate {}", task.getUuid(), delegateId);
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error(format("Error refreshing whitelist entry for task %s", task.getUuid()), e);
    }
  }

  @Override
  public void saveConnectionResults(List<DelegateConnectionResult> results) {
    List<DelegateConnectionResult> resultsToSave =
        results.stream().filter(result -> isNotBlank(result.getCriteria())).collect(toList());

    for (DelegateConnectionResult result : resultsToSave) {
      Key<DelegateConnectionResult> existingResultKey = wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                            .filter("accountId", result.getAccountId())
                                                            .filter("delegateId", result.getDelegateId())
                                                            .filter("criteria", result.getCriteria())
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
    wingsPersistence.delete(
        wingsPersistence.createQuery(DelegateConnectionResult.class).filter("accountId", accountId));
  }

  @Override
  public void clearConnectionResults(String accountId, String delegateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                .filter("accountId", accountId)
                                .filter("delegateId", delegateId));
  }

  @Override
  public String getActiveDelegateAssignmentErrorMessage(DelegateTask delegateTask) {
    logger.info("Delegate task {} is terminated", delegateTask.getUuid());

    String errorMessage = "Unknown";

    try {
      List<String> activeDelegates = accountConnectedDelegatesCache.get(delegateTask.getAccountId());

      logger.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

      List<String> eligibleDelegates = connectedWhitelistedDelegates(delegateTask);

      if (activeDelegates.isEmpty()) {
        errorMessage = "There were no active delegates to complete the task.";
      } else if (eligibleDelegates.isEmpty()) {
        StringBuilder msg = new StringBuilder();
        for (String delegateId : activeDelegates) {
          Delegate delegate = delegateService.get(delegateTask.getAccountId(), delegateId, false);
          if (delegate != null) {
            msg.append(" ===> ").append(delegate.getHostName()).append(": ");
            boolean cannotAssignScope = !canAssignScopes(delegate, delegateTask);
            boolean cannotAssignTags = !canAssignTags(delegate, delegateTask);
            if (cannotAssignScope) {
              msg.append("Not in scope");
            }
            if (cannotAssignScope && cannotAssignTags) {
              msg.append(" - ");
            }
            if (cannotAssignTags) {
              msg.append("Tag mismatch: ").append(Optional.ofNullable(delegate.getTags()).orElse(emptyList()));
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
