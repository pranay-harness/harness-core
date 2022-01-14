package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.NO_AVAILABLE_DELEGATES;

public class NoAvailableDelegatesException extends NoDelegatesException {
  public NoAvailableDelegatesException() {
    super("Delegates are not available", NO_AVAILABLE_DELEGATES);
  }
}
