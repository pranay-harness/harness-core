package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentRollingRollbackSetupRequest extends K8sCommandRequest {
  int releaseNumber;
  @Builder
  public K8sDeploymentRollingRollbackSetupRequest(String accountId, String appId, String commandName, String activityId,
      K8sCommandType k8sCommandType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, int releaseNumber) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sCommandType);
    this.releaseNumber = releaseNumber;
  }
}
