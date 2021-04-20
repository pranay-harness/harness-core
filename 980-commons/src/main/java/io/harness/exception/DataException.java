package io.harness.exception;

import static io.harness.eraro.ErrorCode.DATA;
import static io.harness.eraro.Level.INFO;

public class DataException extends WingsException {
  public DataException(Throwable cause) {
    super(null, cause, DATA, INFO, null, null);
  }
}
