/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_SETUP;

import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstSetupTaskParameters extends SpotInstTaskParameters {
  private String elastiGroupJson;
  private String elastiGroupNamePrefix;
  private boolean blueGreen;
  private String image;
  private String resizeStrategy;
  private List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs;
  private String userData;

  @Builder
  public SpotInstSetupTaskParameters(String accountId, String appId, String commandName, String activityId,
      Integer timeoutIntervalInMin, String elastiGroupJson, String workflowExecutionId, String elastiGroupNamePrefix,
      boolean blueGreen, String image, String resizeStrategy, String awsRegion,
      List<LoadBalancerDetailsForBGDeployment> awsLoadBalancerConfigs, String userData) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin, SPOT_INST_SETUP,
        awsRegion);
    this.blueGreen = blueGreen;
    this.elastiGroupJson = elastiGroupJson;
    this.elastiGroupNamePrefix = elastiGroupNamePrefix;
    this.image = image;
    this.resizeStrategy = resizeStrategy;
    this.awsLoadBalancerConfigs = awsLoadBalancerConfigs;
    this.userData = userData;
  }
}
