package software.wings.helpers.ext.k8s.request;

import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.InstanceUnitType;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import java.util.List;
import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sCanaryDeployTaskParameters extends K8sTaskParameters implements ManifestAwareTaskParams {
  private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  @Expression private List<String> valuesYamlList;
  private Integer instances;
  private InstanceUnitType instanceUnitType;
  private Optional<Integer> maxInstances;
  private boolean skipDryRun;

  @Builder
  public K8sCanaryDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList,
      Integer instances, InstanceUnitType instanceUnitType, Integer maxInstances, boolean skipDryRun,
      HelmVersion helmVersion) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion);
    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
    this.instances = instances;
    this.instanceUnitType = instanceUnitType;
    this.maxInstances = Optional.ofNullable(maxInstances);
    this.skipDryRun = skipDryRun;
  }
}
