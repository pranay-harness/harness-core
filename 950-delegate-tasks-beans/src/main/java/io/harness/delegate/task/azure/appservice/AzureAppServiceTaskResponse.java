/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.appservice;

import io.harness.delegate.task.azure.AzureTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppDeploymentSlotsResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppInstancesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppListWebAppNamesResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppRollbackResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotShiftTrafficResponse;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSwapSlotsResponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureWebAppListWebAppDeploymentSlotsResponse.class,
      name = "azureWebAppListWebAppDeploymentSlotNamesResponse")
  ,
      @JsonSubTypes.Type(value = AzureWebAppListWebAppNamesResponse.class, name = "azureWebAppListWebAppNamesResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSlotShiftTrafficResponse.class, name = "azureWebAppSlotResizeResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSlotSetupResponse.class, name = "azureWebAppSlotSetupResponse"),
      @JsonSubTypes.Type(value = AzureWebAppSwapSlotsResponse.class, name = "azureWebAppSwapSlotsResponse"),
      @JsonSubTypes.Type(value = AzureWebAppRollbackResponse.class, name = "azureWebAppRollbackResponse"),
      @JsonSubTypes.Type(
          value = AzureWebAppListWebAppInstancesResponse.class, name = "azureWebAppListWebAppInstancesResponse")
})
public interface AzureAppServiceTaskResponse extends AzureTaskResponse {}
