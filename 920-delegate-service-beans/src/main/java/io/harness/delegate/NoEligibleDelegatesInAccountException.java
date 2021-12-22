package io.harness.delegate;

import static io.harness.eraro.ErrorCode.NO_AVAILABLE_DELEGATES;

import io.harness.delegate.beans.NoDelegatesException;

public class NoEligibleDelegatesInAccountException extends NoDelegatesException {
  public NoEligibleDelegatesInAccountException() {
    super("No eligible delegates to execute task", NO_AVAILABLE_DELEGATES);
  }
}
