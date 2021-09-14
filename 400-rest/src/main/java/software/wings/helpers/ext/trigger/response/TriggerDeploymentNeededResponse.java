/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.trigger.response;

import io.harness.beans.ExecutionStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerDeploymentNeededResponse extends TriggerResponse {
  private boolean deploymentNeeded;

  @Builder
  public TriggerDeploymentNeededResponse(ExecutionStatus executionStatus, String errorMsg, boolean deploymentNeeded) {
    super(null, executionStatus, errorMsg);
    this.deploymentNeeded = deploymentNeeded;
  }
}
