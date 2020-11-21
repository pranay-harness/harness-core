package software.wings.resources.error;

import io.harness.eraro.ErrorCode;

import lombok.Value;

/**
 * Wrapper class over errorCode and message. Treats errors as values.
 * Can be used when your error does not have any additional information required. Use custom exceptions in those cases.
 */
@Value
public class Error {
  private ErrorCode code;
  private String message;
}
