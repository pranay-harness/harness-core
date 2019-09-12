package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_SETUP;

import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstSetupTaskParameters extends SpotInstTaskParameters {
  private String elastiGroupJson;
  private String elastiGroupNamePrefix;
  private boolean blueGreen;
  private String image;
  private String resizeStrategy;
  private List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs;

  @Builder
  public SpotInstSetupTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, String elastiGroupJson, String workflowExecutionId, String elastiGroupNamePrefix,
      boolean blueGreen, String image, String resizeStrategy, String awsRegion,
      List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_SETUP,
        awsRegion);
    this.blueGreen = blueGreen;
    this.elastiGroupJson = elastiGroupJson;
    this.elastiGroupNamePrefix = elastiGroupNamePrefix;
    this.image = image;
    this.resizeStrategy = resizeStrategy;
    this.awsLoadBalancerConfigs = awsLoadBalancerConfigs;
  }
}