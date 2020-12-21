package io.harness.http;

import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpOutcome implements Outcome {
  String httpUrl;
  String httpMethod;
  int httpResponseCode;
  String httpResponseBody;
  CommandExecutionStatus status;
  String errorMsg;
  Map<String, String> outputVariables;

  @Override
  public String getType() {
    return null;
  }
}
