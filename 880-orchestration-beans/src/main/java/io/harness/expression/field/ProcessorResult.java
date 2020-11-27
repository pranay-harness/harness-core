package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ProcessorResult {
  // Status after processing of a field:
  // - ERROR: There was an error processing the field
  // - UNCHANGED: Processing the field did not result in a value and the expression itself didn't change
  // - CHANGED: Either we got a value (which was not there previously) or expression resulted in another different
  //   expression
  public enum Status { ERROR, UNCHANGED, CHANGED }

  Status status;
  String message;
}
