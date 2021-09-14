/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_LIST_ELASTI_GROUP_INSTANCES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstListElastigroupInstancesParameters extends SpotInstTaskParameters {
  private String elastigroupId;

  @Builder
  public SpotInstListElastigroupInstancesParameters(String appId, String accountId, String activityId,
      String commandName, String workflowExecutionId, Integer timeoutIntervalInMin, String awsRegion,
      String elastigroupId) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin,
        SPOT_INST_LIST_ELASTI_GROUP_INSTANCES, awsRegion);
    this.elastigroupId = elastigroupId;
  }
}
