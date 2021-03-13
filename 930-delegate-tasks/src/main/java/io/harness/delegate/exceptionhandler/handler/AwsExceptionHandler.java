package io.harness.delegate.exceptionhandler.handler;

import io.harness.exception.WingsException;

public class AwsExceptionHandler implements DelegateExceptionHandler {
  @Override
  public WingsException handleException(Exception exception) {
    return null;
  }
}
