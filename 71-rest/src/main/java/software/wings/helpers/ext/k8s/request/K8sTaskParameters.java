package software.wings.helpers.ext.k8s.request;

import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@AllArgsConstructor
public class K8sTaskParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private K8sClusterConfig k8sClusterConfig;
  private String workflowExecutionId;
  private String releaseName;
  private Integer timeoutIntervalInMin;
  @NotEmpty private K8sTaskType commandType;
  private HelmVersion helmVersion;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return k8sClusterConfig.fetchRequiredExecutionCapabilities();
  }

  public enum K8sTaskType {
    DEPLOYMENT_ROLLING,
    DEPLOYMENT_ROLLING_ROLLBACK,
    SCALE,
    CANARY_DEPLOY,
    BLUE_GREEN_DEPLOY,
    INSTANCE_SYNC,
    DELETE,
    TRAFFIC_SPLIT,
    APPLY
  }
}
