package software.wings.api;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsLambdaExecutionData extends StateExecutionData implements NotifyResponseData {
  private String functionName;
  private String functionArn;
  private String functionVersion;
  private Integer statusCode;
  private String functionError;
  private String logResult;
  private String payload;
  private boolean executionDisabled;
  private String assertionStatement;
  private String assertionStatus;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();
    putNotNull(executionDetails, "functionName",
        ExecutionDataValue.builder().displayName("Function Name").value(functionName).build());
    putNotNull(executionDetails, "functionArn",
        ExecutionDataValue.builder().displayName("Function ARN").value(functionArn).build());
    putNotNull(executionDetails, "functionVersion",
        ExecutionDataValue.builder().displayName("Function Version").value(functionVersion).build());
    putNotNull(executionDetails, "executionDisabled",
        ExecutionDataValue.builder().displayName("Execution Disabled").value(executionDisabled).build());
    putNotNull(executionDetails, "statusCode",
        ExecutionDataValue.builder().displayName("Status Code").value(statusCode).build());
    putNotNull(executionDetails, "functionError",
        ExecutionDataValue.builder().displayName("Function Error").value(functionError).build());
    putNotNull(executionDetails, "payload", ExecutionDataValue.builder().displayName("Payload").value(payload).build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    putNotNull(
        executionDetails, "logResult", ExecutionDataValue.builder().displayName("Log Result").value(logResult).build());
    return executionDetails;
  }
}
