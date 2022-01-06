/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
