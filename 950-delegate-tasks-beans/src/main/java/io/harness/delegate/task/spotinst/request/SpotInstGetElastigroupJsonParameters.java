/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.spotinst.request;

import static io.harness.delegate.task.spotinst.request.SpotInstTaskParameters.SpotInstTaskType.SPOT_INST_GET_ELASTI_GROUP_JSON;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotInstGetElastigroupJsonParameters extends SpotInstTaskParameters {
  private String elastigroupId;

  @Builder
  public SpotInstGetElastigroupJsonParameters(String appId, String accountId, String activityId, String commandName,
      String workflowExecutionId, Integer timeoutIntervalInMin, String elastigroupId) {
    super(appId, accountId, activityId, commandName, workflowExecutionId, timeoutIntervalInMin,
        SPOT_INST_GET_ELASTI_GROUP_JSON, "us-east-1");
    this.elastigroupId = elastigroupId;
  }
}
