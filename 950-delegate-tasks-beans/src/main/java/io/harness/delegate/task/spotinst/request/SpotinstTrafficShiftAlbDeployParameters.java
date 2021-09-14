/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_ALB_SHIFT_DEPLOY;

import io.harness.spotinst.model.ElastiGroup;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotinstTrafficShiftAlbDeployParameters extends SpotInstTaskParameters {
  private ElastiGroup oldElastigroup;
  private ElastiGroup newElastigroup;

  @Builder
  public SpotinstTrafficShiftAlbDeployParameters(String appId, String accountId, String activityId, String commandName,
      String workflowExecutionId, Integer timeoutIntervalInMin, String awsRegion, ElastiGroup newElastigroup,
      ElastiGroup oldElastigroup) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin,
        SPOT_INST_ALB_SHIFT_DEPLOY, awsRegion);
    this.newElastigroup = newElastigroup;
    this.oldElastigroup = oldElastigroup;
  }
}
