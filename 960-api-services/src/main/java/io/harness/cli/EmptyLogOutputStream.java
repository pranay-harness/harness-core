/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cli;

import org.zeroturnaround.exec.stream.LogOutputStream;

public class EmptyLogOutputStream extends LogOutputStream {
  @Override
  protected void processLine(String s) {
    // Not Logging so that secrets are not exposed
  }
}
