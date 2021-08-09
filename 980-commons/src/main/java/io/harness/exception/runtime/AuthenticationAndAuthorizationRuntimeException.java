package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AuthenticationAndAuthorizationRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;

  public AuthenticationAndAuthorizationRuntimeException(String message) {
    this.message = message;
  }

  public AuthenticationAndAuthorizationRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }
}
