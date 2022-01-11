/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.ecs;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.EcsSetUpDataBag;

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
public class EcsBGRoute53SetupStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private String commandName;
  private TaskType taskType;
  private Map<K8sValuesLocation, ApplicationManifest> applicationManifestMap;
  private EcsSetUpDataBag ecsSetUpDataBag;
  private String roleArn;
  private String targetPort;
  private String maxInstances;
  private String fixedInstances;
  private String ecsServiceName;
  private String targetContainerName;
  private String desiredInstanceCount;
  private int serviceSteadyStateTimeout;
  private ResizeStrategy resizeStrategy;
  private List<AwsAutoScalarConfig> awsAutoScalarConfigs;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;

  private String parentRecordName;
  private String parentRecordHostedZoneId;
  private String serviceDiscoveryService1JSON;
  private String serviceDiscoveryService2JSON;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> execDetails = super.getExecutionDetails();
    putNotNull(execDetails, "commandName",
        ExecutionDataValue.builder().value(commandName).displayName("Command Name").build());
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(
        execDetails, "activityId", ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return execDetails;
  }

  @Override
  public EcsListenerUpdateExecutionSummary getStepExecutionSummary() {
    return EcsListenerUpdateExecutionSummary.builder().build();
  }
}
