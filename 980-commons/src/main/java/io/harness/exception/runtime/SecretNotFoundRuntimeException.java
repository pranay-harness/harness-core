package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class SecretNotFoundRuntimeException extends SecretRuntimeException {
  public SecretNotFoundRuntimeException(String message) {
    super(message);
  }

  public SecretNotFoundRuntimeException(String message, String secretRef, String scope, String connectorRef) {
    super(message, secretRef, scope, connectorRef);
  }

  public SecretNotFoundRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
