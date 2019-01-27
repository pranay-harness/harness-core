package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.sm.StateExecutionData;

import java.util.Map;

@Data
@Builder
public class ShellScriptApprovalExecutionData extends StateExecutionData implements ResponseData {
  private ExecutionStatus executionStatus;
  private Action approvalAction;
  private String activityId;
  private Map<String, String> sweepingOutputEnvVariables;
  private String name;
  private String approvalId;
  private String errorMessage;

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
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder().displayName("Activity Id").value(activityId).build());
    putNotNull(executionDetails, "name", ExecutionDataValue.builder().displayName("Name").value(name).build());

    putNotNull(executionDetails, "approvalAction",
        ExecutionDataValue.builder().displayName("Approval Action").value(approvalAction).build());
    putNotNull(executionDetails, "executionStatus",
        ExecutionDataValue.builder().displayName("Execution Status").value(executionStatus).build());

    if (isNotEmpty(sweepingOutputEnvVariables)) {
      putNotNull(executionDetails, "sweepingOutputEnvVariables",
          ExecutionDataValue.builder()
              .displayName("Script Output")
              .value(removeNullValues(sweepingOutputEnvVariables))
              .build());
    }
  }

  @Override
  public ScriptStateExecutionSummary getStepExecutionSummary() {
    return ScriptStateExecutionSummary.builder()
        .activityId(activityId)
        .sweepingOutputEnvVariables(sweepingOutputEnvVariables)
        .build();
  }
}
