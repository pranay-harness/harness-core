package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class AuthenticationRuntimeException extends AuthenticationAndAuthorizationRuntimeException {
  public AuthenticationRuntimeException(String message) {
    super(message);
  }

  public AuthenticationRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
