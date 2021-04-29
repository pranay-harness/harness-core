package io.harness.eraro;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

/**
 * Wrapper class over errorCode and message. Treats errors as values.
 * Can be used when your error does not have any additional information required. Use custom exceptions in those cases.
 */
@Value
@OwnedBy(HarnessTeam.PL)
public class Error {
  private ErrorCode code;
  private String message;
}
