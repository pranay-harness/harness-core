package io.harness.pms.sdk.core.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringOutcome implements Outcome {
  String message;

  // TODO: Why this get type is needed
  @Override
  public String getType() {
    return "__stringOutcome__";
  }
}
