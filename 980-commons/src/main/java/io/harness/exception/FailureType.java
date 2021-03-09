package io.harness.exception;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._955_DELEGATE_BEANS)
public enum FailureType {
  EXPIRED(""),
  DELEGATE_PROVISIONING(""),
  CONNECTIVITY(""),
  AUTHENTICATION(""),
  VERIFICATION_FAILURE(""),
  APPLICATION_ERROR(""),
  AUTHORIZATION_ERROR(""),
  TIMEOUT_ERROR("");

  String errorMessage;

  FailureType(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  String getErrorMessageFromType(Exception e) {
    return this.errorMessage + " due to: " + e.getMessage();
  }
}
