/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import org.junit.runners.model.Statement;

public class NoopStatement extends Statement {
  @Override
  public void evaluate() throws Throwable {
    // noop
  }
}
