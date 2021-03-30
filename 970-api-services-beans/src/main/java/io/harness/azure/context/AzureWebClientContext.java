package io.harness.azure.context;

import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;

import io.harness.azure.model.AzureConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureWebClientContext extends AzureClientContext {
  @NotBlank(message = WEB_APP_NAME_BLANK_ERROR_MSG) private String appName;

  @Builder
  AzureWebClientContext(@NonNull AzureConfig azureConfig, @NonNull String subscriptionId,
      @NonNull String resourceGroupName, @NotBlank(message = WEB_APP_NAME_BLANK_ERROR_MSG) String appName) {
    super(azureConfig, subscriptionId, resourceGroupName);
    this.appName = appName;
  }
}
