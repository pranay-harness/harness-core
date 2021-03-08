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
