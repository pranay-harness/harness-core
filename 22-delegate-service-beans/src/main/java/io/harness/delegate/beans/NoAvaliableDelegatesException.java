package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class NoAvaliableDelegatesException extends WingsException {
  public NoAvaliableDelegatesException() {
    super("Delegates are not available", null, RESOURCE_NOT_FOUND, Level.ERROR, USER, null);
  }
}
