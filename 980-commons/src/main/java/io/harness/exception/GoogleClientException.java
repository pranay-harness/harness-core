package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

@OwnedBy(CDP)
public class GoogleClientException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GoogleClientException(String message, ErrorCode errorCode) {
    super(message, null, errorCode, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GoogleClientException(String message, ErrorCode errorCode, Throwable cause) {
    super(message, cause, errorCode, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }
}
