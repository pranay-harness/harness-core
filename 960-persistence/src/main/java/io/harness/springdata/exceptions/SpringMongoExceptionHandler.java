package io.harness.springdata.exceptions;

import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

public class SpringMongoExceptionHandler implements ExceptionHandler {
  @Override
  public WingsException handleException(Exception exception) {
    return new WingsTransactionFailureException(exception.getMessage(), WingsException.USER);
  }
}
