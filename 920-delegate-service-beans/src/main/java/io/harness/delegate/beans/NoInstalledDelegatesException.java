package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.NO_INSTALLED_DELEGATES;

public class NoInstalledDelegatesException extends NoDelegatesException {
  public NoInstalledDelegatesException() {
    super("No installed delegates found", NO_INSTALLED_DELEGATES);
  }
}
