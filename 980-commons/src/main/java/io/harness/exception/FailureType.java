package io.harness.exception;

public enum FailureType {
  EXPIRED(""),
  DELEGATE_PROVISIONING(""),
  CONNECTIVITY(""),
  AUTHENTICATION(""),
  VERIFICATION_FAILURE(""),
  APPLICATION_ERROR(""),
  AUTHORIZATION_ERROR("");

  String errorMessage;

  FailureType(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  String getErrorMessageFromType(Exception e) {
    return this.errorMessage + " due to: " + e.getMessage();
  }
}
