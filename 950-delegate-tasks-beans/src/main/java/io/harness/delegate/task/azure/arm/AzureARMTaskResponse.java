/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.arm;

import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.arm.response.AzureARMDeploymentResponse;
import io.harness.delegate.task.azure.arm.response.AzureARMRollbackResponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureARMDeploymentResponse.class, name = "azureARMDeploymentResponse")
  , @JsonSubTypes.Type(value = AzureARMRollbackResponse.class, name = "azureARMRollbackResponse")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AzureARMTaskResponse implements AzureTaskResponse {
  private String errorMsg;
}
