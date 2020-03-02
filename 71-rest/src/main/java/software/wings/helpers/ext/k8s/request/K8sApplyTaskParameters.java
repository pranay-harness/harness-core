package software.wings.helpers.ext.k8s.request;

import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sApplyTaskParameters extends K8sTaskParameters implements ManifestAwareTaskParams {
  private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  @Expression private List<String> valuesYamlList;

  private String filePaths;
  private boolean skipSteadyStateCheck;
  private boolean skipDryRun;

  @Builder
  public K8sApplyTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList,
      String filePaths, boolean skipSteadyStateCheck, boolean skipDryRun, HelmVersion helmVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion);

    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
    this.filePaths = filePaths;
    this.skipSteadyStateCheck = skipSteadyStateCheck;
    this.skipDryRun = skipDryRun;
  }
}
