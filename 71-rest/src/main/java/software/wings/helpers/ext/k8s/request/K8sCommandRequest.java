package software.wings.helpers.ext.k8s.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class K8sCommandRequest {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private K8sClusterConfig k8sClusterConfig;
  private String workflowExecutionId;
  private String infraMappingId;
  private Integer timeoutIntervalInMin;
  @NotEmpty private K8sCommandType commandType;
  public enum K8sCommandType {
    DEPLOYMENT_ROLLING,
    DEPLOYMENT_ROLLING_ROLLBACK;
  }
}
