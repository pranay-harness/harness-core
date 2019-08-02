package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstSetupTaskParameters extends SpotInstTaskParameters {
  private int stageListenerPort;
  private String targetListenerProtocol;
  private String elastiGroupJson;
  private String elastiGroupNamePrefix;
  private boolean blueGreen;
  private String image;
  private String resizeStrategy;
  private String loadBalancerName;
  private boolean classicLoadBalancer;
  private int prodListenerPort;

  @Builder
  public SpotInstSetupTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, String elastiGroupJson, String workflowExecutionId, String elastiGroupNamePrefix,
      int stageListenerPort, String targetListenerProtocol, boolean blueGreen, String image, String resizeStrategy,
      String loadBalancerName, String awsRegion, boolean classicLoadBalancer, int prodListenerPort) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_SETUP,
        awsRegion);
    this.stageListenerPort = stageListenerPort;
    this.targetListenerProtocol = targetListenerProtocol;
    this.blueGreen = blueGreen;
    this.elastiGroupJson = elastiGroupJson;
    this.elastiGroupNamePrefix = elastiGroupNamePrefix;
    this.image = image;
    this.resizeStrategy = resizeStrategy;
    this.loadBalancerName = loadBalancerName;
    this.classicLoadBalancer = classicLoadBalancer;
    this.prodListenerPort = prodListenerPort;
  }
}