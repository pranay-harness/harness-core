package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskProperties;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;

/**
 * Created by brett on 7/20/17
 */
@Singleton
public class AssignDelegateServiceImpl implements AssignDelegateService {
  @Inject private DelegateService delegateService;

  @Override
  public boolean assign(DelegateTask task, String delegateId, String accountId) {
    Delegate delegate = delegateService.get(accountId, delegateId);
    DelegateTaskProperties taskProperties = delegate.getTaskProperties();
    return task == null || task.getEnvId() == null || taskProperties.getEnvironments().isEmpty()
        || taskProperties.getEnvironments().contains(task.getEnvId());
  }
}
