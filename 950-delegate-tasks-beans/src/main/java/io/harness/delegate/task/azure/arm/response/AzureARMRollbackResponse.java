/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMRollbackResponse extends AzureARMTaskResponse {
  @Builder
  public AzureARMRollbackResponse(String errorMsg) {
    super(errorMsg);
  }
}
