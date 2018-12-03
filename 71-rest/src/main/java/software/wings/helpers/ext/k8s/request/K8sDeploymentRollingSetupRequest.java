package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentRollingSetupRequest extends K8sCommandRequest {
  private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  private List<String> valuesYamlList;

  @Builder
  public K8sDeploymentRollingSetupRequest(String accountId, String appId, String commandName, String activityId,
      K8sCommandType k8sCommandType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sCommandType);
    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
  }
}
