package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.delegatetasks.k8s.watch.WatchRequest;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sAddWatchTaskParameters extends K8sTaskParameters {
  private WatchRequest watchRequest;

  @Builder
  public K8sAddWatchTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, WatchRequest watchRequest) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType);

    this.watchRequest = watchRequest;
  }
}
