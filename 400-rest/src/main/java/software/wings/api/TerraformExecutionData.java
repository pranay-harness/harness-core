/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.task.terraform.TerraformCommand;
import io.harness.provision.TfVarSource;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.beans.NameValuePair;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class TerraformExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String activityId;

  private ExecutionStatus executionStatus;
  private String errorMessage;

  private String entityId;
  private String stateFileId;

  private String outputs;
  private TerraformCommand commandExecuted;
  private List<NameValuePair> variables;
  private List<NameValuePair> backendConfigs;
  private List<NameValuePair> environmentVariables;

  private String sourceRepoReference;
  private List<String> targets;
  private List<String> tfVarFiles;
  private TfVarSource tfVarSource;

  private String workspace;
  private String delegateTag;

  private String tfPlanJson;
  private EncryptedRecordData encryptedTfPlan;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
