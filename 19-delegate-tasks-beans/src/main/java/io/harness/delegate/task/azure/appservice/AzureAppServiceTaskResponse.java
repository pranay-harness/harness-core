package io.harness.delegate.task.azure.appservice;

import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureWebAppListWebAppDeploymentSlotNamesResponse.class,
      name = "azureWebAppListWebAppDeploymentSlotNamesResponse")
  ,
      @JsonSubTypes.Type(value = AzureWebAppListWebAppNamesResponse.class, name = "azureWebAppListWebAppNamesResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSlotShiftTrafficResponse.class, name = "azureWebAppSlotResizeResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSlotSetupResponse.class, name = "azureWebAppSlotSetupResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSwapSlotsResponse.class, name = "azureWebAppSwapSlotsResponse"),
})
public interface AzureAppServiceTaskResponse extends AzureTaskResponse {}
