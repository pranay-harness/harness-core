/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.NO_INSTALLED_DELEGATES;

public class NoInstalledDelegatesException extends NoDelegatesException {
  public NoInstalledDelegatesException() {
    super("No installed delegates found", NO_INSTALLED_DELEGATES);
  }
}
