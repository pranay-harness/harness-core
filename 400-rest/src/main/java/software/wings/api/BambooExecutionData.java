/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.sm.StateExecutionData;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.sm.states.ParameterEntry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BambooExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String projectName;
  private String planName;
  private String buildStatus;
  private String buildUrl;
  private String buildNumber;
  private String planUrl;
  private List<ParameterEntry> parameters;
  private List<FilePathAssertionEntry> filePathAssertionEntries;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    setBambooExecutionDetails(executionDetails);
    return executionDetails;
  }

  private void setBambooExecutionDetails(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "projectName",
        ExecutionDataValue.builder().displayName("Project Name").value(projectName).build());

    putNotNull(
        executionDetails, "planName", ExecutionDataValue.builder().displayName("Plan Name").value(planName).build());

    if (isNotEmpty(parameters)) {
      Map<String, String> parameterMap = isEmpty(parameters)
          ? Collections.emptyMap()
          : parameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
      putNotNull(executionDetails, "parameters",
          ExecutionDataValue.builder().displayName("Parameters").value(removeNullValues(parameterMap)).build());
    }
    putNotNull(executionDetails, "fileAssertionData",
        ExecutionDataValue.builder().displayName("Assertion Data").value(filePathAssertionEntries).build());

    putNotNull(executionDetails, "buildNumber",
        ExecutionDataValue.builder().displayName("Build Number").value(buildNumber).build());

    putNotNull(executionDetails, "buildStatus",
        ExecutionDataValue.builder().displayName("Build Status").value(buildStatus).build());

    putNotNull(
        executionDetails, "buildUrl", ExecutionDataValue.builder().displayName("Build Url").value(buildUrl).build());
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    setBambooExecutionDetails(executionDetails);
    return executionDetails;
  }
}
