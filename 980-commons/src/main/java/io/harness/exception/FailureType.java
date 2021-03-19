package io.harness.exception;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.EnumSet;

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

  public static final EnumSet<FailureType> TIMEOUT = EnumSet.of(TIMEOUT_ERROR);

  FailureType(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  String getErrorMessageFromType(Exception e) {
    return this.errorMessage + " due to: " + e.getMessage();
  }
}
