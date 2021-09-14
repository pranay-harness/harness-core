/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMDeploymentResponse extends AzureARMTaskResponse {
  private String outputs;
  private AzureARMPreDeploymentData preDeploymentData;

  @Builder
  public AzureARMDeploymentResponse(String outputs, AzureARMPreDeploymentData preDeploymentData, String errorMsg) {
    super(errorMsg);
    this.outputs = outputs;
    this.preDeploymentData = preDeploymentData;
  }
}
