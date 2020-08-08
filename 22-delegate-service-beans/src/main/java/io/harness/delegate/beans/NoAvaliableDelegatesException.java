package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.NO_AVAILABLE_DELEGATES;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class NoAvaliableDelegatesException extends WingsException {
  public NoAvaliableDelegatesException() {
    super("Delegates are not available", null, NO_AVAILABLE_DELEGATES, Level.ERROR, USER, null);
  }
}
