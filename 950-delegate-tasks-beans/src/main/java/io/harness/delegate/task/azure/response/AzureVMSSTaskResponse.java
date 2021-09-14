/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AzureVMSSListSubscriptionsResponse.class, name = "azureVMSSubscriptionsResponse")
  ,
      @JsonSubTypes.Type(value = AzureVMSSGetVirtualMachineScaleSetResponse.class,
          name = "azureVMSSGetVirtualMachineScaleSetDataResponse"),
      @JsonSubTypes.Type(
          value = AzureVMSSListResourceGroupsNamesResponse.class, name = "azureVMSSListResourceGroupsNamesResponse"),
      @JsonSubTypes.Type(value = AzureVMSSListVirtualMachineScaleSetsResponse.class,
          name = "azureVMSSListVirtualMachineScaleSetsResponse"),
      @JsonSubTypes.Type(value = AzureVMSSSetupTaskResponse.class, name = "azureVMSSSetupTaskResponse"),
      @JsonSubTypes.Type(value = AzureVMSSDeployTaskResponse.class, name = "azureVMSSDeployTaskResponse"),
      @JsonSubTypes.Type(value = AzureVMSSListVMDataResponse.class, name = "azureVMSSListVMDataResponse"),
      @JsonSubTypes.Type(value = AzureVMSSSwitchRoutesResponse.class, name = "azureVMSSSwitchRoutesResponse")
})
public interface AzureVMSSTaskResponse {}
