package io.harness.steps.http;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDC)
@RecasterAlias("io.harness.steps.http.HttpOutcome")
public class HttpOutcome implements Outcome {
  String httpUrl;
  String httpMethod;
  int httpResponseCode;
  String httpResponseBody;
  CommandExecutionStatus status;
  String errorMsg;
  Map<String, String> outputVariables;
}
