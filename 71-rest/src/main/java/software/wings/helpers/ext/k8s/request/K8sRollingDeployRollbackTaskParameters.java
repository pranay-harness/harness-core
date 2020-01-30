package software.wings.helpers.ext.k8s.request;

import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sRollingDeployRollbackTaskParameters extends K8sTaskParameters {
  Integer releaseNumber;
  @Builder
  public K8sRollingDeployRollbackTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, Integer releaseNumber, HelmVersion helmVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion);
    this.releaseNumber = releaseNumber;
  }
}
