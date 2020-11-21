package software.wings.api;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.NameValuePair;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
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

  private String planLogFileId;
  private String workspace;
  private String delegateTag;

  private byte[] tfPlanFile;
  private String tfPlanJson;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
