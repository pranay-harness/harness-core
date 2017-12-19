package software.wings.service.impl;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.TaskGroup;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by brett on 7/20/17
 */
@Singleton
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private DelegateService delegateService;
  @Inject private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean canAssign(String delegateId, DelegateTask task) {
    return canAssign(delegateId, task.getAccountId(), task.getAppId(), task.getEnvId(),
        task.getInfrastructureMappingId(), task.getTaskType() != null ? task.getTaskType().getTaskGroup() : null);
  }

  @Override
  public boolean canAssign(
      String delegateId, String accountId, String appId, String envId, String infraMappingId, TaskGroup taskGroup) {
    Delegate delegate = delegateService.get(accountId, delegateId);
    if (delegate == null) {
      return false;
    }
    boolean assign = delegate.getIncludeScopes() == null || delegate.getIncludeScopes().isEmpty();
    if (delegate.getIncludeScopes() != null) {
      for (DelegateScope delegateScope : delegate.getIncludeScopes()) {
        if (scopeMatch(delegateScope, appId, envId, infraMappingId, taskGroup)) {
          assign = true;
          break;
        }
      }
    }
    if (assign && delegate.getExcludeScopes() != null) {
      for (DelegateScope delegateScope : delegate.getExcludeScopes()) {
        if (scopeMatch(delegateScope, appId, envId, infraMappingId, taskGroup)) {
          assign = false;
          break;
        }
      }
    }
    return assign;
  }

  private boolean scopeMatch(
      DelegateScope delegateScope, String appId, String envId, String infraMappingId, TaskGroup taskGroup) {
    if (!delegateScope.isValid()) {
      logger.error("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Delegate scope cannot be empty.");
    }
    boolean match = true;

    if (isNotEmpty(delegateScope.getEnvironmentTypes())) {
      match = isNotBlank(appId) && isNotBlank(envId)
          && delegateScope.getEnvironmentTypes().contains(
                 environmentService.get(appId, envId, false).getEnvironmentType());
    }
    if (match && isNotEmpty(delegateScope.getTaskTypes())) {
      match = delegateScope.getTaskTypes().contains(taskGroup);
    }
    if (match && isNotEmpty(delegateScope.getApplications())) {
      match = isNotBlank(appId) && delegateScope.getApplications().contains(appId);
    }
    if (match && isNotEmpty(delegateScope.getEnvironments())) {
      match = isNotBlank(envId) && delegateScope.getEnvironments().contains(envId);
    }
    if (match && isNotEmpty(delegateScope.getServiceInfrastructures())) {
      match = isNotBlank(infraMappingId) && delegateScope.getServiceInfrastructures().contains(infraMappingId);
    }

    return match;
  }

  @Override
  public boolean isWhitelisted(DelegateTask task, String delegateId) {
    try {
      for (String criteria : task.getTaskType().getCriteria(task)) {
        if (isNotBlank(criteria)) {
          DelegateConnectionResult result = wingsPersistence.createQuery(DelegateConnectionResult.class)
                                                .field("delegateId")
                                                .equal(delegateId)
                                                .field("criteria")
                                                .equal(criteria)
                                                .get();
          if (result != null && result.isValidated()) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error checking whether delegate is whitelisted for task", e);
    }
    return false;
  }

  @Override
  public void saveConnectionResults(List<DelegateConnectionResult> results) {
    List<DelegateConnectionResult> resultsToSave =
        results.stream().filter(result -> isNotBlank(result.getCriteria())).collect(Collectors.toList());
    resultsToSave.forEach(result
        -> result.setUuid(Optional
                              .ofNullable(wingsPersistence.createQuery(DelegateConnectionResult.class)
                                              .field("accountId")
                                              .equal(result.getAccountId())
                                              .field("delegateId")
                                              .equal(result.getDelegateId())
                                              .field("criteria")
                                              .equal(result.getCriteria())
                                              .get())
                              .orElse(result)
                              .getUuid()));
    wingsPersistence.saveIgnoringDuplicateKeys(resultsToSave);
  }

  @Override
  public void clearConnectionResults(String delegateId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(DelegateConnectionResult.class).field("delegateId").equal(delegateId));
  }
}
