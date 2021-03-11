package io.harness.delegate.exceptionhandler.handler;

import io.harness.delegate.exceptionhandler.DelegateExceptionHandlerType;
import io.harness.delegate.exceptionhandler.DelegateExceptionManager;
import io.harness.exception.WingsException;

import com.amazonaws.AmazonServiceException;

public class AwsExceptionHandler implements DelegateExceptionHandler {
  static {
    DelegateExceptionManager.registerHandler(
        AmazonServiceException.class, new AwsExceptionHandler(), DelegateExceptionHandlerType.CURR_GEN);
  }

  // guava modules
  // map bindings

  @Override
  public WingsException handleException(Exception exception) {
    return null;
  }
}
