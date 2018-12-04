package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.InstanceUnitType;

import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sScaleRequest extends K8sCommandRequest {
  private String resource;
  private int instances;
  private InstanceUnitType instanceUnitType;
  private Optional<Integer> maxInstances;
  @Builder
  public K8sScaleRequest(String accountId, String appId, String commandName, String activityId,
      K8sCommandType k8sCommandType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId, String releaseName,
      Integer timeoutIntervalInMin, String resource, int instances, InstanceUnitType instanceUnitType,
      Integer maxInstances) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, releaseName,
        timeoutIntervalInMin, k8sCommandType);
    this.resource = resource;
    this.instances = instances;
    this.instanceUnitType = instanceUnitType;
    this.maxInstances = Optional.ofNullable(maxInstances);
  }
}
