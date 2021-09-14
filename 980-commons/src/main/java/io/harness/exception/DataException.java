/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.DATA;

/**
 * This exception serves as super class for all exceptions to be used to store metadata for error
 * handling framework
 */
public abstract class DataException extends FrameworkBaseException {
  public DataException(Throwable cause) {
    super(cause, DATA);
  }
}
