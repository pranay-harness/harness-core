/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
