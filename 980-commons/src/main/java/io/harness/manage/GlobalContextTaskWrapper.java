/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GlobalContextTaskWrapper implements Runnable {
  private Runnable task;
  private GlobalContext context;

  @Override
  public void run() {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      task.run();
    }
  }
}
