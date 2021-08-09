package io.harness.azure.context;

import io.harness.azure.model.AzureConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebClientContext extends AzureClientContext {
  private String appName;

  @Builder
  AzureWebClientContext(@NonNull AzureConfig azureConfig, @NonNull String subscriptionId,
      @NonNull String resourceGroupName, @NonNull String appName) {
    super(azureConfig, subscriptionId, resourceGroupName);
    this.appName = appName;
  }
}
