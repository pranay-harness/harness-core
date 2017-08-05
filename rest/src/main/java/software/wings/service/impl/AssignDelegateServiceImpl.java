package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;

/**
 * Created by brett on 7/20/17
 */
@Singleton
public class AssignDelegateServiceImpl implements AssignDelegateService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private DelegateService delegateService;
  @Inject private EnvironmentService environmentService;

  @Override
  public boolean canAssign(DelegateTask task, String delegateId) {
    if (task == null || task.getEnvId() == null) {
      return true;
    }
    Delegate delegate = delegateService.get(task.getAccountId(), delegateId);
    boolean assign = delegate.getIncludeScopes().isEmpty();
    Environment env = environmentService.get(task.getAppId(), task.getEnvId(), false);
    for (DelegateScope delegateScope : delegate.getIncludeScopes()) {
      if (scopeMatch(delegateScope, task, env)) {
        assign = true;
        break;
      }
    }
    if (assign) {
      for (DelegateScope delegateScope : delegate.getExcludeScopes()) {
        if (scopeMatch(delegateScope, task, env)) {
          assign = false;
          break;
        }
      }
    }
    return assign;
  }

  private boolean scopeMatch(DelegateScope delegateScope, DelegateTask task, Environment env) {
    if (delegateScope.isEmpty()) {
      logger.error("Delegate scope cannot be empty.");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "message", "Delegate scope cannot be empty.");
    }
    boolean match = true;

    if (!delegateScope.getEnvironmentTypes().isEmpty()) {
      match = delegateScope.getEnvironmentTypes().contains(env.getEnvironmentType());
    }
    if (match && !delegateScope.getApplications().isEmpty()) {
      match = delegateScope.getApplications().contains(task.getAppId());
    }
    if (match && !delegateScope.getEnvironments().isEmpty()) {
      match = delegateScope.getEnvironments().contains(task.getEnvId());
    }
    if (match && !delegateScope.getServiceInfrastructures().isEmpty()) {
      match = delegateScope.getServiceInfrastructures().contains(task.getInfrastructureMappingId());
    }
    if (match && !delegateScope.getTaskTypes().isEmpty()) {
      // TODO: Check task type from task
      // match = delegateScope.getServiceInfrastructures().contains(task.get());
    }

    return match;
  }
}
