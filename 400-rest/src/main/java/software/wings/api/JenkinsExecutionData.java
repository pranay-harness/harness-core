/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.StateExecutionData;
import software.wings.sm.states.FilePathAssertionEntry;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by peeyushaggarwal on 10/24/16.
 */
@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class JenkinsExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String jobName;
  private String jobStatus;
  private String buildUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap;
  private Map<String, String> jobParameters;
  private Map<String, String> envVars;
  private String activityId;
  private Map<String, String> metadata;
  private String buildNumber;
  private String buildDisplayName;
  private String buildFullDisplayName;
  private String description;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setExecutionData(executionDetails);
    return executionDetails;
  }

  private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(
        executionDetails, "jobName", ExecutionDataValue.builder().displayName("Job Name").value(jobName).build());

    if (isNotEmpty(jobParameters)) {
      putNotNull(executionDetails, "jobParameters",
          ExecutionDataValue.builder().displayName("Job Parameters").value(removeNullValues(jobParameters)).build());
    }

    if (isNotEmpty(envVars)) {
      putNotNull(executionDetails, "envVars",
          ExecutionDataValue.builder().displayName("Environment Variables").value(removeNullValues(envVars)).build());
    }

    putNotNull(executionDetails, "fileAssertionData",
        ExecutionDataValue.builder().displayName("Assertion Data").value(filePathAssertionMap).build());
    putNotNull(
        executionDetails, "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value(jobStatus).build());
    putNotNull(executionDetails, "buildNumber",
        ExecutionDataValue.builder().displayName("Build Number").value(buildNumber).build());

    putNotNull(executionDetails, "buildDisplayName",
        ExecutionDataValue.builder().displayName("Build DisplayName").value(buildDisplayName).build());

    putNotNull(executionDetails, "buildFullDisplayName",
        ExecutionDataValue.builder().displayName("Build Full DisplayName").value(buildFullDisplayName).build());

    putNotNull(executionDetails, "description",
        ExecutionDataValue.builder().displayName("Description").value(description).build());
    putNotNull(
        executionDetails, "build", ExecutionDataValue.builder().displayName("Build Url").value(buildUrl).build());
    if (isNotEmpty(metadata)) {
      putNotNull(executionDetails, "metadata",
          ExecutionDataValue.builder()
              .displayName("Meta-Data")
              .value(String.valueOf(removeNullValues(metadata)))
              .build());
    }
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
  }
}
