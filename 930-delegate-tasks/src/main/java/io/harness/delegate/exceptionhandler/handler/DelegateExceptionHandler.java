/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.exceptionhandler.handler;

import io.harness.exception.WingsException;

public interface DelegateExceptionHandler {
  WingsException handleException(Exception exception);
}
