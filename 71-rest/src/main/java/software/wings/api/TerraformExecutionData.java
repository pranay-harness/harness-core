package software.wings.api;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class TerraformExecutionData extends StateExecutionData implements ResponseData {
  private String activityId;

  private ExecutionStatus executionStatus;
  private String errorMessage;

  private String entityId;
  private String stateFileId;

  private String outputs;
  private TerraformCommand commandExecuted;
  private List<NameValuePair> variables;
  private List<NameValuePair> backendConfigs;

  private String sourceRepoReference;

  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
