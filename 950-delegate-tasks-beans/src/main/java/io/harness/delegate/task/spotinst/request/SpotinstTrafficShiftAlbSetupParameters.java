/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_ALB_SHIFT_SETUP;

import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotinstTrafficShiftAlbSetupParameters extends SpotInstTaskParameters {
  private String elastigroupJson;
  private String elastigroupNamePrefix;
  private String image;
  private List<LbDetailsForAlbTrafficShift> lbDetails;
  private String userData;

  @Builder
  public SpotinstTrafficShiftAlbSetupParameters(String appId, String accountId, String activityId, String commandName,
      String workflowExecutionId, Integer timeoutIntervalInMin, String awsRegion, String elastigroupJson,
      String elastigroupNamePrefix, String image, List<LbDetailsForAlbTrafficShift> lbDetails, String userData) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin,
        SPOT_INST_ALB_SHIFT_SETUP, awsRegion);
    this.elastigroupJson = elastigroupJson;
    this.elastigroupNamePrefix = elastigroupNamePrefix;
    this.image = image;
    this.lbDetails = lbDetails;
    this.userData = userData;
  }
}
