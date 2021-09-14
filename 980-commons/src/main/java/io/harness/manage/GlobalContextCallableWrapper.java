/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

import java.util.concurrent.Callable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GlobalContextCallableWrapper<T> implements Callable<T> {
  private Callable<T> task;
  private GlobalContext context;

  @Override
  public T call() throws Exception {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      return task.call();
    }
  }
}
