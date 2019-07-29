package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstSetupTaskParameters extends SpotInstTaskParameters {
  private int port;
  private String protocol;
  private String elastiGroupJson;
  private String elastiGroupNamePrefix;

  @Builder
  public SpotInstSetupTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, String elastiGroupJson, String workflowExecutionId, String elastiGroupNamePrefix,
      int port, String protocol) {
    super(accountId, appId, commandName, workflowExecutionId, activityId, timeoutIntervalInMin, SPOT_INST_SETUP);
    this.port = port;
    this.protocol = protocol;
    this.elastiGroupJson = elastiGroupJson;
    this.elastiGroupNamePrefix = elastiGroupNamePrefix;
  }
}