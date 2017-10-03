package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import software.wings.sm.StateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

public class AwsLambdaExecutionData extends StateExecutionData implements NotifyResponseData {
  private String functionArn;
  private String functionVersion;
  private Integer statusCode;
  private String functionError;
  private String logResult;
  private String payload;

  public AwsLambdaExecutionData() {}

  public String getFunctionArn() {
    return functionArn;
  }

  public void setFunctionArn(String functionArn) {
    this.functionArn = functionArn;
  }

  public String getFunctionVersion() {
    return functionVersion;
  }

  public void setFunctionVersion(String functionVersion) {
    this.functionVersion = functionVersion;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public String getFunctionError() {
    return functionError;
  }

  public void setFunctionError(String functionError) {
    this.functionError = functionError;
  }

  public String getLogResult() {
    return logResult;
  }

  public void setLogResult(String logResult) {
    this.logResult = logResult;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  public Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();
    putNotNull(executionDetails, "functionArn",
        anExecutionDataValue().withValue(functionArn).withDisplayName("Function ARN").build());
    putNotNull(executionDetails, "functionVersion",
        anExecutionDataValue().withValue(functionVersion).withDisplayName("Function Version").build());
    putNotNull(executionDetails, "statusCode",
        anExecutionDataValue().withValue(statusCode).withDisplayName("Status Code").build());
    putNotNull(executionDetails, "functionError",
        anExecutionDataValue().withValue(functionError).withDisplayName("Function Error").build());
    putNotNull(
        executionDetails, "payload", anExecutionDataValue().withValue(payload).withDisplayName("Payload").build());
    putNotNull(executionDetails, "logResult",
        anExecutionDataValue().withValue(logResult).withDisplayName("Log Result").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }
}
