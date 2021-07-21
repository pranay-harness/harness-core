package software.wings.helpers.ext.k8s.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.HelmVersion;

import software.wings.beans.InstanceUnitType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class K8sCanaryDeployTaskParameters extends K8sTaskParameters implements ManifestAwareTaskParams {
  private Boolean skipVersioningForAllK8sObjects;
  @Expression(ALLOW_SECRETS) private K8sDelegateManifestConfig k8sDelegateManifestConfig;
  @Expression(ALLOW_SECRETS) private List<String> valuesYamlList;
  private Integer instances;
  private InstanceUnitType instanceUnitType;
  private Optional<Integer> maxInstances;
  private boolean skipDryRun;

  @Builder
  public K8sCanaryDeployTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, K8sDelegateManifestConfig k8sDelegateManifestConfig, List<String> valuesYamlList,
      Integer instances, InstanceUnitType instanceUnitType, Integer maxInstances, boolean skipDryRun,
      HelmVersion helmVersion, Boolean skipVersioningForAllK8sObjects, Set<String> delegateSelectors) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType, helmVersion, delegateSelectors);
    this.k8sDelegateManifestConfig = k8sDelegateManifestConfig;
    this.valuesYamlList = valuesYamlList;
    this.instances = instances;
    this.instanceUnitType = instanceUnitType;
    this.maxInstances = Optional.ofNullable(maxInstances);
    this.skipDryRun = skipDryRun;
    this.skipVersioningForAllK8sObjects = skipVersioningForAllK8sObjects;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities =
        new ArrayList<>(super.fetchRequiredExecutionCapabilities(maskingEvaluator));

    Set<String> delegateSelectors = getDelegateSelectorsFromConfigs(k8sDelegateManifestConfig);
    if (isNotEmpty(delegateSelectors)) {
      capabilities.add(SelectorCapability.builder().selectors(delegateSelectors).build());
    }
    return capabilities;
  }
}
