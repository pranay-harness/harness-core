/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.appservice;

import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceApplicationSettingDTO;
import io.harness.delegate.beans.azure.appservicesettings.AzureAppServiceConnectionStringDTO;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppServicePreDeploymentData {
  private String appName;
  private String slotName;
  private double trafficWeight;
  private String deploymentProgressMarker;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToRemove;
  private Map<String, AzureAppServiceApplicationSettingDTO> appSettingsToAdd;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToRemove;
  private Map<String, AzureAppServiceConnectionStringDTO> connStringsToAdd;
  private Map<String, AzureAppServiceApplicationSettingDTO> dockerSettingsToAdd;
  private String imageNameAndTag;
  private AzureAppServiceTaskType failedTaskType;
}
