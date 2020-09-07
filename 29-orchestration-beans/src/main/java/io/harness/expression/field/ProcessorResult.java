package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ProcessorResult {
  boolean success;
  String message;
  Object value;
}
