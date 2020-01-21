package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeleteTaskParameters extends K8sTaskParameters {
  private String resources;
  private boolean deleteNamespacesForRelease;
  @Builder
  public K8sDeleteTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, String resources, boolean deleteNamespacesForRelease) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType);
    this.resources = resources;
    this.deleteNamespacesForRelease = deleteNamespacesForRelease;
  }
}
