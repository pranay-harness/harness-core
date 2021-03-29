package io.harness.exception.exceptionmanager;

import io.harness.exception.WingsException;

public interface ExceptionHandler {
  WingsException handleException(Exception exception);
}
