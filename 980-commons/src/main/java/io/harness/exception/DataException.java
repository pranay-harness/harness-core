package io.harness.exception;

import static io.harness.eraro.ErrorCode.DATA;
import static io.harness.eraro.Level.INFO;

/**
 * This exception serves as super class for all exceptions to be used to store metadata for error
 * handling framework
 */
public abstract class DataException extends WingsException {
  public DataException(Throwable cause) {
    super(null, cause, DATA, INFO, null, null);
  }
}
