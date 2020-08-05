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
      @JsonSubTypes.Type(value = AzureVMSSTaskSetupResponse.class, name = "azureVMSSSetupTaskResponse"),
      @JsonSubTypes.Type(value = AzureVMSSDeployTaskResponse.class, name = "azureVMSSDeployTaskResponse")
})
public interface AzureVMSSTaskResponse {}
