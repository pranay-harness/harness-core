/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class AutoLogRemoveContext implements AutoCloseable {
  private Map<String, String> originals;

  public AutoLogRemoveContext(String... keys) {
    for (String key : keys) {
      String original = MDC.get(key);
      if (original != null) {
        if (originals == null) {
          originals = new HashMap<>();
        }

        originals.put(key, original);
        MDC.remove(key);
      }
    }
  }

  @Override
  public void close() {
    if (isEmpty(originals)) {
      return;
    }
    for (Entry<String, String> entry : originals.entrySet()) {
      MDC.put(entry.getKey(), entry.getValue());
    }
  }
}
