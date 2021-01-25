package io.harness.delegate.task.k8s;

import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.beans.NGInstanceUnitType;
import io.harness.expression.Expression;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sScaleRequest implements K8sDeployRequest {
  @Expression(DISALLOW_SECRETS) String releaseName;
  String commandName;
  K8sTaskType taskType;
  Integer timeoutIntervalInMin;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  String workload;
  Integer instances;
  NGInstanceUnitType instanceUnitType;
  Optional<Integer> maxInstances;
  boolean skipSteadyStateCheck;
}
