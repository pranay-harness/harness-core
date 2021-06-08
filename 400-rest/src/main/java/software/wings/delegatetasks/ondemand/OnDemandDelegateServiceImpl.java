package software.wings.delegatetasks.ondemand;

import software.wings.delegatetasks.ondemand.OnDemandDelegateTask.OnDemandDelegateTaskKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;

public class OnDemandDelegateServiceImpl implements OnDemandDelegateService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void enqueue(String accountId, String kubeconfig, String delegateYaml) {
    OnDemandDelegateTask task = OnDemandDelegateTask.builder()
                                    .accountId(accountId)
                                    .delegateYaml(delegateYaml)
                                    .kubeConfig(kubeconfig)
                                    .status(OnDemandDelegateTask.OnDemandExecStatus.QUEUED)
                                    .build();
    wingsPersistence.save(task);
  }

  @Override
  public OnDemandDelegateTask getNextTask(String accountId) {
    return wingsPersistence.createAuthorizedQuery(OnDemandDelegateTask.class)
        .filter(OnDemandDelegateTaskKeys.accountId, accountId)
        .filter(OnDemandDelegateTaskKeys.status, OnDemandDelegateTask.OnDemandExecStatus.QUEUED)
        .get();
  }
}
