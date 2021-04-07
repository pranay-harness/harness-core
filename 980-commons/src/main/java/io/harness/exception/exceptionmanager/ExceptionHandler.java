package io.harness.exception.exceptionmanager;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;

@OwnedBy(HarnessTeam.DX)
public interface ExceptionHandler {
  WingsException handleException(Exception exception);
}
