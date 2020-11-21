package software.wings.helpers.ext.k8s.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.expression.Expression;
import io.harness.k8s.model.HelmVersion;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sRollingDeployTaskParameters extends K8sTaskParameters implements ManifestAwareTaskParams {
  private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  @Expression(ALLOW_SECRETS) private List<String> valuesYamlList;
  private boolean isInCanaryWorkflow;
  private boolean skipDryRun;
  private boolean localOverrideFeatureFlag;

  @Builder
  public K8sRollingDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList,
      boolean isInCanaryWorkflow, boolean skipDryRun, HelmVersion helmVersion, boolean localOverrideFeatureFlag,
      boolean deprecateFabric8Enabled) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion, deprecateFabric8Enabled);
    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
    this.isInCanaryWorkflow = isInCanaryWorkflow;
    this.skipDryRun = skipDryRun;
    this.localOverrideFeatureFlag = localOverrideFeatureFlag;
  }
}
