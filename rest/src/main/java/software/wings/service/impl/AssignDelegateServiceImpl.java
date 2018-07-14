package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.MAX_DELEGATE_LAST_HEARTBEAT;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 7/20/17
 */
@Singleton
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(AssignDelegateServiceImpl.class);

  private static final long WHITELIST_TTL = TimeUnit.HOURS.toMillis(6);

  @Inject private DelegateService delegateService;
  @Inject private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private Injector injector;

  @Override
  public boolean canAssign(String delegateId, DelegateTask task) {
    return canAssign(delegateId, task.getAccountId(), task.getAppId(), task.getEnvId(),
        task.getInfrastructureMappingId(),
        isNotBlank(task.getTaskType()) ? TaskType.valueOf(task.getTaskType()).getTaskGroup() : null, task.getTags());
  }

  @Override
  public boolean canAssign(String delegateId, String accountId, String appId, String envId, String infraMappingId,
      TaskGroup taskGroup, List<String> tags) {
    Delegate delegate = delegateService.get(accountId, delegateId);
    if (delegate == null) {
      return false;
    }
    return (isEmpty(delegate.getIncludeScopes())
               || delegate.getIncludeScopes().stream().anyMatch(
                      scope -> scopeMatch(scope, appId, envId, infraMappingId, taskGroup, tags)))
        && (isEmpty(delegate.getExcludeScopes())
               || delegate.getExcludeScopes().stream().noneMatch(
                      scope -> scopeMatch(scope, appId, envId, infraMappingId, taskGroup, tags)));
  }

  private boolean scopeMatch(
      DelegateScope scope, String appId, String envId, String infraMappingId, TaskGroup taskGroup, List<String> tags) {
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
    if (match && isNotEmpty(scope.getTags())) {
      // Match any tag. If it needs to match all tags change to:
      // match = isNotEmpty(tags) && scope.getTags().containsAll(tags);
      match = isNotEmpty(tags) && tags.stream().anyMatch(tag -> scope.getTags().contains(tag));
    }

    return match;
  }

  @Override
  public boolean isWhitelisted(DelegateTask task, String delegateId) {
    try {
      for (String criteria : TaskType.valueOf(task.getTaskType()).getCriteria(task, injector)) {
        if (isNotBlank(criteria)) {
          DelegateConnectionResult result = wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                .filter("accountId", task.getAccountId())
                                                .filter("delegateId", delegateId)
                                                .filter("criteria", criteria)
                                                .field("lastUpdatedAt")
                                                .greaterThan(clock.millis() - WHITELIST_TTL)
                                                .get();
          if (result != null && result.isValidated()) {
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
  public List<String> connectedWhitelistedDelegates(DelegateTask task) {
    List<String> delegateIds = new ArrayList<>();
    try {
      List<String> connectedEligibleDelegates = wingsPersistence.createQuery(Delegate.class)
                                                    .filter("accountId", task.getAccountId())
                                                    .field("lastHeartBeat")
                                                    .greaterThan(clock.millis() - MAX_DELEGATE_LAST_HEARTBEAT)
                                                    .asKeyList()
                                                    .stream()
                                                    .map(key -> key.getId().toString())
                                                    .filter(delegateId -> canAssign(delegateId, task))
                                                    .collect(toList());

      for (String criteria : TaskType.valueOf(task.getTaskType()).getCriteria(task, injector)) {
        if (isNotBlank(criteria)) {
          delegateIds.addAll(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                 .filter("accountId", task.getAccountId())
                                 .filter("criteria", criteria)
                                 .filter("validated", true)
                                 .field("delegateId")
                                 .in(connectedEligibleDelegates)
                                 .project("delegateId", true)
                                 .asList()
                                 .stream()
                                 .map(DelegateConnectionResult::getDelegateId)
                                 .collect(toList()));
        }
      }
    } catch (Exception e) {
      logger.error("Error checking for whitelisted delegates for task {}", task.getUuid(), e);
    }
    return delegateIds;
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
  public void clearConnectionResults(String delegateId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(DelegateConnectionResult.class).filter("delegateId", delegateId));
  }
}
