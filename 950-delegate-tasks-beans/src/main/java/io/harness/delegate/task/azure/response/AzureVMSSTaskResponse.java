/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
