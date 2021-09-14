/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.NO_AVAILABLE_DELEGATES;

public class NoAvailableDelegatesException extends NoDelegatesException {
  public NoAvailableDelegatesException() {
    super("Delegates are not available", NO_AVAILABLE_DELEGATES);
  }
}
