package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sCanaryRollbackTaskParameters extends K8sTaskParameters {
  Integer releaseNumber;
  Integer targetReplicas;
  @Builder
  public K8sCanaryRollbackTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, Integer releaseNumber, Integer targetReplicas, HelmVersion helmVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion);
    this.releaseNumber = releaseNumber;
    this.targetReplicas = targetReplicas;
  }
}
