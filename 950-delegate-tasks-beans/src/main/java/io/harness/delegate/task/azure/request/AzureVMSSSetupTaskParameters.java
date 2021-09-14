/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.request;

import static io.harness.delegate.task.azure.request.AzureVMSSTaskParameters.AzureVMSSTaskType.AZURE_VMSS_SETUP;

import io.harness.delegate.beans.azure.AzureMachineImageArtifactDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureVMSSSetupTaskParameters extends AzureVMSSTaskParameters {
  private boolean blueGreen;
  private String vmssNamePrefix;
  private String baseVMSSName;
  private String subscriptionId;
  private String resourceGroupName;
  private String userData;
  private AzureMachineImageArtifactDTO imageArtifactDTO;
  private AzureVMAuthDTO azureVmAuthDTO;
  private List<EncryptedDataDetail> vmAuthDTOEncryptionDetails;
  private int minInstances;
  private int maxInstances;
  private int desiredInstances;
  private int autoScalingSteadyStateVMSSTimeout;
  private boolean useCurrentRunningCount;
  private String vmssDeploymentType;
  private String infraMappingId;
  private AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail;

  @Builder
  public AzureVMSSSetupTaskParameters(String appId, String accountId, String activityId, String commandName,
      Integer timeoutIntervalInMin, AzureVMSSTaskType commandType, boolean blueGreen, String vmssNamePrefix,
      AzureMachineImageArtifactDTO imageArtifactDTO, String baseVMSSName, String subscriptionId,
      String resourceGroupName, String userData, AzureVMAuthDTO azureVmAuthDTO,
      List<EncryptedDataDetail> vmAuthDTOEncryptionDetails, int minInstances, int maxInstances, int desiredInstances,
      int autoScalingSteadyStateVMSSTimeout, boolean useCurrentRunningCount, String vmssDeploymentType,
      String infraMappingId, AzureLoadBalancerDetailForBGDeployment azureLoadBalancerDetail) {
    super(appId, accountId, activityId, commandName, timeoutIntervalInMin, AZURE_VMSS_SETUP);
    this.blueGreen = blueGreen;
    this.vmssNamePrefix = vmssNamePrefix;
    this.imageArtifactDTO = imageArtifactDTO;
    this.baseVMSSName = baseVMSSName;
    this.subscriptionId = subscriptionId;
    this.resourceGroupName = resourceGroupName;
    this.userData = userData;
    this.azureVmAuthDTO = azureVmAuthDTO;
    this.vmAuthDTOEncryptionDetails = vmAuthDTOEncryptionDetails;
    this.minInstances = minInstances;
    this.maxInstances = maxInstances;
    this.desiredInstances = desiredInstances;
    this.autoScalingSteadyStateVMSSTimeout = autoScalingSteadyStateVMSSTimeout;
    this.useCurrentRunningCount = useCurrentRunningCount;
    this.vmssDeploymentType = vmssDeploymentType;
    this.infraMappingId = infraMappingId;
    this.azureLoadBalancerDetail = azureLoadBalancerDetail;
  }
}
