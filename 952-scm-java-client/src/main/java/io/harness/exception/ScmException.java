package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

@OwnedBy(DX)
public class ScmException extends WingsException {
  public ScmException(ErrorCode errorCode) {
    super("", null, errorCode, Level.ERROR, USER, null);
  }
}
