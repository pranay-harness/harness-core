package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

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
        anExecutionDataValue().withValue(functionName).withDisplayName("Function Name").build());
    putNotNull(executionDetails, "functionArn",
        anExecutionDataValue().withValue(functionArn).withDisplayName("Function ARN").build());
    putNotNull(executionDetails, "functionVersion",
        anExecutionDataValue().withValue(functionVersion).withDisplayName("Function Version").build());
    putNotNull(executionDetails, "executionDisabled",
        anExecutionDataValue().withValue(executionDisabled).withDisplayName("Execution Disabled").build());
    putNotNull(executionDetails, "statusCode",
        anExecutionDataValue().withValue(statusCode).withDisplayName("Status Code").build());
    putNotNull(executionDetails, "functionError",
        anExecutionDataValue().withValue(functionError).withDisplayName("Function Error").build());
    putNotNull(
        executionDetails, "payload", anExecutionDataValue().withValue(payload).withDisplayName("Payload").build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    putNotNull(executionDetails, "logResult",
        anExecutionDataValue().withValue(logResult).withDisplayName("Log Result").build());
    return executionDetails;
  }
}
