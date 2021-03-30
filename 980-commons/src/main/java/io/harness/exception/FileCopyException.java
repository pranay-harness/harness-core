package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

@OwnedBy(CDP)
public class FileCopyException extends WingsException {
  public static final String MESSAGE_KEY = "message";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public FileCopyException(String message) {
    super(null, null, ErrorCode.GENERAL_ERROR, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public FileCopyException(String message, Throwable cause) {
    super(null, cause, ErrorCode.GENERAL_ERROR, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }
}
