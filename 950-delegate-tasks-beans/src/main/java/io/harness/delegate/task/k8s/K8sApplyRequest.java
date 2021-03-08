package io.harness.delegate.task.k8s;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.expression.Expression;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sApplyRequest implements K8sDeployRequest {
  boolean skipDryRun;
  @Expression(DISALLOW_SECRETS) String releaseName;
  String commandName;
  K8sTaskType taskType;
  Integer timeoutIntervalInMin;
  @Expression(ALLOW_SECRETS) List<String> valuesYamlList;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  boolean deprecateFabric8Enabled;
  String accountId;
  boolean skipSteadyStateCheck;
  List<String> filePaths;
}