/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.task.pcf.CfCommandRequest;

import software.wings.api.ExecutionDataValue;
import software.wings.api.pcf.PcfPluginExecutionSummary.PcfPluginExecutionSummaryBuilder;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.StateExecutionData;

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
@OwnedBy(CDP)
@TargetModule(_870_CG_ORCHESTRATION)
public class PcfPluginStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;
  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private EnvironmentType environmentType;
  private String infraMappingId;
  private String commandName;
  private TaskType taskType;
  private Map<K8sValuesLocation, ApplicationManifest> appManifestMap;
  private GitFetchFilesFromMultipleRepoResult fetchFilesResult;
  private CfCommandRequest pcfCommandRequest;
  private List<String> filePathsInScript;
  private String renderedScriptString;
  private Integer timeoutIntervalInMinutes;
  private List<String> tagList;
  private String repoRoot;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }
  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();

    if (pcfCommandRequest != null) {
      putNotNull(executionDetails, "space",
          ExecutionDataValue.builder().value(pcfCommandRequest.getSpace()).displayName("Space").build());
      putNotNull(executionDetails, "organization",
          ExecutionDataValue.builder().value(pcfCommandRequest.getOrganization()).displayName("Organization").build());
    }

    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().value(activityId).displayName("Activity Id").build());

    return executionDetails;
  }

  @Override
  public PcfPluginExecutionSummary getStepExecutionSummary() {
    final PcfPluginExecutionSummaryBuilder builder = PcfPluginExecutionSummary.builder();

    if (pcfCommandRequest != null) {
      builder.organization(pcfCommandRequest.getOrganization()).space(pcfCommandRequest.getSpace());
    }

    return builder.build();
  }
}
