package io.harness.http;

import static io.harness.annotations.dev.HarnessTeam.NG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(NG)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpStepParameters implements StepParameters {
  String url;
  String method;
  Map<String, String> requestHeaders;
  String requestBody;
  String assertion;
  Map<String, String> outputVariables;
  int timeout;
}
