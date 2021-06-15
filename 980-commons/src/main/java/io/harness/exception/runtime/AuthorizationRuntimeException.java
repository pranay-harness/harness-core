package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class AuthorizationRuntimeException extends AuthenticationAndAuthorizationRuntimeException {
  public AuthorizationRuntimeException(String message) {
    super(message);
  }

  public AuthorizationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
