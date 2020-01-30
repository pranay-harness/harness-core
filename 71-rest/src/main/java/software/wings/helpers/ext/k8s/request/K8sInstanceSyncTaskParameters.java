package software.wings.helpers.ext.k8s.request;

import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sInstanceSyncTaskParameters extends K8sTaskParameters {
  String namespace;
  @Builder
  public K8sInstanceSyncTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName, Integer timeoutIntervalInMin,
      String namespace, HelmVersion helmVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, K8sTaskType.INSTANCE_SYNC, helmVersion);
    this.namespace = namespace;
  }
}
