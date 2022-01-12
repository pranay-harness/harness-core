/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.resource;

import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.resource.operation.AzureResourceOperationResponse;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AzureResourceTaskResponse implements AzureTaskResponse {
  private AzureResourceOperationResponse operationResponse;
  private String errorMsg;

  @Builder
  public AzureResourceTaskResponse(AzureResourceOperationResponse operationResponse, String errorMsg) {
    this.operationResponse = operationResponse;
    this.errorMsg = errorMsg;
  }
}
