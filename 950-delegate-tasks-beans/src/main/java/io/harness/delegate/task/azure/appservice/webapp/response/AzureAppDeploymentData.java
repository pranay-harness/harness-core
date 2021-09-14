/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.appservice.webapp.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureAppDeploymentData {
  private String instanceId;
  private String instanceType;
  private String instanceName;
  private String resourceGroup;
  private String subscriptionId;
  private String appName;
  private String deploySlot;
  private String deploySlotId;
  private String appServicePlanId;
  private String hostName;
  private String instanceIp;
  private String instanceState;
}
