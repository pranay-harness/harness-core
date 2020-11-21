package io.harness.azure.context;

import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class AzureClientContext {
  @NonNull private AzureConfig azureConfig;
  @NonNull private String subscriptionId;
  @NonNull private String resourceGroupName;
}
