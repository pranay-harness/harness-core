package software.wings.delegatetasks.ondemand;

public interface OnDemandDelegateService {
  void enqueue(String accountId, String kubeconfig, String delegateYaml);
  OnDemandDelegateTask getNextTask(String accountId);
}
