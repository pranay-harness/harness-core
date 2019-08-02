package io.harness.delegate.task.spotinst.request;

import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@AllArgsConstructor
public class SpotInstTaskParameters implements TaskParameters {
  private String appId;
  private String accountId;
  private String activityId;
  private String commandName;
  private String workflowExecutionId;
  private Integer timeoutIntervalInMin;
  @NotEmpty private SpotInstTaskType commandType;
  @NotEmpty private String awsRegion;

  public enum SpotInstTaskType { SPOT_INST_SETUP, SPOT_INST_DEPLOY, SPOT_INST_SWAP_ROUTES }
}
