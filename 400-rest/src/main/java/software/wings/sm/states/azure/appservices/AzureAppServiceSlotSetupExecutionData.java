/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.azure.appservices;

import static io.harness.azure.model.AzureConstants.ACTIVITY_ID;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AzureAppServiceSlotSetupExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String infrastructureMappingId;
  private Integer appServiceSlotSetupTimeOut;
  private String subscriptionId;
  private String resourceGroup;
  private String appServiceName;
  private String deploySlotName;
  private String targetSlotName;
  private String webAppUrl;
  private boolean isGitFetchDone;
  private TaskType taskType;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;
  private Map<String, ApplicationManifest> appServiceConfigurationManifests;

  @Builder.Default private List<InstanceStatusSummary> newInstanceStatusSummaries = new ArrayList<>();

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "resourceGroup",
        ExecutionDataValue.builder().displayName("Resource Group").value(resourceGroup).build());
    putNotNull(executionDetails, "appServiceName",
        ExecutionDataValue.builder().displayName("Web App Name").value(appServiceName).build());
    putNotNull(executionDetails, "deploySlotName",
        ExecutionDataValue.builder().displayName("Deployment Slot").value(deploySlotName).build());
    putNotNull(executionDetails, "targetSlotName",
        ExecutionDataValue.builder().displayName("Target Slot").value(targetSlotName).build());
    putNotNull(
        executionDetails, "webAppUrl", ExecutionDataValue.builder().displayName("WebApp URL").value(webAppUrl).build());
    putNotNull(
        executionDetails, ACTIVITY_ID, ExecutionDataValue.builder().displayName(ACTIVITY_ID).value(activityId).build());
    return executionDetails;
  }

  @Override
  public AzureAppServiceSlotSetupExecutionSummary getStepExecutionSummary() {
    return AzureAppServiceSlotSetupExecutionSummary.builder()
        .newSlotName(deploySlotName)
        .newAppName(appServiceName)
        .build();
  }
}
