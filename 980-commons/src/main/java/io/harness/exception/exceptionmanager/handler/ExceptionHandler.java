package io.harness.exception.exceptionmanager.handler;

import io.harness.exception.WingsException;

public interface ExceptionHandler {
  WingsException handleException(Exception exception);
}
