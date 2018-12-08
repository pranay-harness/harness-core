package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.InstanceUnitType;

import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sScaleTaskParameters extends K8sTaskParameters {
  private String resource;
  private int instances;
  private InstanceUnitType instanceUnitType;
  private Optional<Integer> maxInstances;
  private boolean skipSteadyStateCheck;
  @Builder
  public K8sScaleTaskParameters(String accountId, String appId, String commandName, String activityId,
      K8sTaskType k8sTaskType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, String resource, int instances, InstanceUnitType instanceUnitType,
      Integer maxInstances, boolean skipSteadyStateCheck) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sTaskType);
    this.resource = resource;
    this.instances = instances;
    this.instanceUnitType = instanceUnitType;
    this.maxInstances = Optional.ofNullable(maxInstances);
    this.skipSteadyStateCheck = skipSteadyStateCheck;
  }
}
