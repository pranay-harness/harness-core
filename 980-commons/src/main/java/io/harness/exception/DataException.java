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
