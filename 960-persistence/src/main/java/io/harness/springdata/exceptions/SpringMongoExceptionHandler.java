/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.springdata.exceptions;

import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

public class SpringMongoExceptionHandler implements ExceptionHandler {
  @Override
  public WingsException handleException(Exception exception) {
    return new WingsTransactionFailureException(exception.getMessage(), WingsException.USER);
  }
}
