package io.harness.exception;

import static io.harness.eraro.Level.INFO;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

/**
 * This exception serves as base class for all dedicated exceptions added to support
 * error handling framework
 */
@OwnedBy(HarnessTeam.DX)
public abstract class FrameworkBaseException extends WingsException {
  public FrameworkBaseException(Throwable cause, ErrorCode errorCode) {
    super(null, cause, errorCode, INFO, null, null);
  }
}
