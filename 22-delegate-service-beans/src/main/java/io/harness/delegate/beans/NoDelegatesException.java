package io.harness.delegate.beans;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public abstract class NoDelegatesException extends WingsException {
  public NoDelegatesException(String message, ErrorCode errorCode) {
    super(message, null, errorCode, Level.ERROR, USER, null);
  }
}
