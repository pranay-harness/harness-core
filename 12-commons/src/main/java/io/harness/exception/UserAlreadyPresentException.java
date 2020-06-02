package io.harness.exception;

import static io.harness.eraro.ErrorCode.USER_ALREADY_PRESENT;

import io.harness.eraro.Level;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class UserAlreadyPresentException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public UserAlreadyPresentException(String message) {
    super(message, null, USER_ALREADY_PRESENT, Level.INFO, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
