/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.context;

import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ARMDeploymentSteadyStateContext {
  private String tenantId;
  private String managementGroupId;
  private String subscriptionId;
  private String resourceGroup;
  private String deploymentName;
  private AzureConfig azureConfig;
  private ARMScopeType scopeType;
  private long steadyCheckTimeoutInMinutes;
  private long statusCheckIntervalInSeconds;
}
