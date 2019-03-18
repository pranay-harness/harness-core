package software.wings.api.shellscript.provision;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
public class ShellScriptProvisionExecutionData extends StateExecutionData implements ResponseData {
  private ExecutionStatus executionStatus;
  private String errorMsg;
  private String output;
  private String activityId;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "activityId", ExecutionDataValue.builder().displayName("").value(activityId).build());
    return executionDetails;
  }
}
