package io.harness.delegate.exceptionhandler.handler;

import io.harness.exception.WingsException;

public interface DelegateExceptionHandler {
  WingsException handleException(Exception exception);
}
