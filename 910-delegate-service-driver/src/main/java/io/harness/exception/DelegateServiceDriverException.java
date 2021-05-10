package io.harness.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceDriverException extends RuntimeException {
  public DelegateServiceDriverException(String message, Throwable cause) {
    super(message, cause);
  }

  public DelegateServiceDriverException(String message) {
    super(message);
  }
}
