package software.wings.helpers.ext.k8s.request;

import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class K8sTaskParameters implements TaskParameters {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private K8sClusterConfig k8sClusterConfig;
  private String workflowExecutionId;
  private String releaseName;
  private Integer timeoutIntervalInMin;
  @NotEmpty private K8sTaskType commandType;
  public enum K8sTaskType {
    DEPLOYMENT_ROLLING,
    DEPLOYMENT_ROLLING_ROLLBACK,
    SCALE,
    CANARY_DEPLOY,
    BLUE_GREEN_DEPLOY,
    INSTANCE_SYNC,
    DELETE;
  }
}
