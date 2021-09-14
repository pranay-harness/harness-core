/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class AutoLogRemoveAllContext implements AutoCloseable {
  private Map<String, String> original;

  public AutoLogRemoveAllContext() {
    original = MDC.getCopyOfContextMap();
    MDC.clear();
  }

  @Override
  public void close() {
    if (original != null) {
      MDC.setContextMap(original);
    }
  }
}
