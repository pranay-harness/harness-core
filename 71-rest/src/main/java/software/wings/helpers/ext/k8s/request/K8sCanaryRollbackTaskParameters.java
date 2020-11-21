package software.wings.helpers.ext.k8s.request;

import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.k8s.model.HelmVersion;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sCanaryRollbackTaskParameters extends K8sTaskParameters {
  Integer releaseNumber;
  Integer targetReplicas;
  @Builder
  public K8sCanaryRollbackTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, Integer releaseNumber, Integer targetReplicas, HelmVersion helmVersion,
      boolean deprecateFabric8Enabled) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion, deprecateFabric8Enabled);
    this.releaseNumber = releaseNumber;
    this.targetReplicas = targetReplicas;
  }
}
