/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import org.slf4j.MDC;

@OwnedBy(HarnessTeam.DX)
public class MdcContextSetter implements AutoCloseable {
  Map<String, String> contexts;

  public MdcContextSetter(Map<String, String> contexts) {
    if (isEmpty(contexts)) {
      return;
    }
    this.contexts = contexts;
    for (Map.Entry<String, String> context : contexts.entrySet()) {
      MDC.put(context.getKey(), context.getValue());
    }
  }

  @Override
  public void close() {
    if (isEmpty(contexts)) {
      return;
    }
    for (Map.Entry<String, String> context : contexts.entrySet()) {
      MDC.remove(context.getKey());
    }
  }
}
