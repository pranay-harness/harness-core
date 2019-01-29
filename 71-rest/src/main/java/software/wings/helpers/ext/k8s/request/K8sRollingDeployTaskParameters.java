package software.wings.helpers.ext.k8s.request;

import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sRollingDeployTaskParameters extends K8sTaskParameters {
  private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  @Expression private List<String> valuesYamlList;
  private boolean isInCanaryWorkflow;

  @Builder
  public K8sRollingDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList,
      boolean isInCanaryWorkflow) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType);
    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
    this.isInCanaryWorkflow = isInCanaryWorkflow;
  }
}
